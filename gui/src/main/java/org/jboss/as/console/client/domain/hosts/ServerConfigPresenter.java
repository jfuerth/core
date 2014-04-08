/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.domain.hosts;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;
import static org.jboss.dmr.client.ModelDescriptionConstants.ADD;
import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.dmr.client.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.dmr.client.ModelDescriptionConstants.JVM;
import static org.jboss.dmr.client.ModelDescriptionConstants.OP;
import static org.jboss.dmr.client.ModelDescriptionConstants.OUTCOME;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;
import static org.jboss.dmr.client.ModelDescriptionConstants.STEPS;
import static org.jboss.dmr.client.ModelDescriptionConstants.SUCCESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableView;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.domain.events.StaleModelEvent;
import org.jboss.as.console.client.domain.model.Host;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.general.model.LoadSocketBindingsCmd;
import org.jboss.as.console.client.shared.general.model.SocketBinding;
import org.jboss.as.console.client.shared.jvm.CreateJvmCmd;
import org.jboss.as.console.client.shared.jvm.DeleteJvmCmd;
import org.jboss.as.console.client.shared.jvm.Jvm;
import org.jboss.as.console.client.shared.jvm.JvmManagement;
import org.jboss.as.console.client.shared.jvm.UpdateJvmCmd;
import org.jboss.as.console.client.shared.model.ModelAdapter;
import org.jboss.as.console.client.shared.properties.CreatePropertyCmd;
import org.jboss.as.console.client.shared.properties.DeletePropertyCmd;
import org.jboss.as.console.client.shared.properties.NewPropertyWizard;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.as.console.client.shared.state.HostList;
import org.jboss.as.console.client.shared.state.HostSelectionChanged;
import org.jboss.as.console.client.shared.state.ServerConfigList;
import org.jboss.as.console.client.shared.util.DMRUtil;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;

/**
 * @author Heiko Braun
 * @date 3/3/11
 */
public class ServerConfigPresenter extends Presenter<ServerConfigPresenter.MyView, ServerConfigPresenter.MyProxy>
        implements ServerWizardEvent.ServerWizardListener, JvmManagement, PropertyManagement,
        HostSelectionChanged.ChangeListener {

    private final HostInformationStore hostInfoStore;
    private final ServerGroupStore serverGroupStore;

    private DefaultWindow window = null;
    private List<ServerGroupRecord> serverGroups;

    private DefaultWindow propertyWindow;
    private final DispatchAsync dispatcher;
    private final ApplicationMetaData propertyMetaData;
    private final BeanFactory factory;
    private final PlaceManager placeManager;

    private final LoadSocketBindingsCmd loadSocketCmd;
    private final DomainEntityManager domainManager;


    @ProxyCodeSplit
    @NameToken(NameTokens.ServerPresenter)
    @OperationMode(DOMAIN)
    @AccessControl(resources = {
            "/{selected.host}/server-config=*",
            "opt://{selected.host}/server-config=*/system-property=*"
    }, recursive = false)
    public interface MyProxy extends Proxy<ServerConfigPresenter>, Place {


    }

    public interface MyView extends SuspendableView {

        void setPresenter(ServerConfigPresenter presenter);

        void updateSocketBindings(List<String> result);

        void setJvm(String reference, Jvm jvm);

        void setProperties(String reference, List<PropertyRecord> properties);

        void setPorts(String socketBinding, Server selectedRecord, List<SocketBinding> result);

        void setConfigurations(ServerConfigList serverList);

        void setGroups(List<ServerGroupRecord> result);

        void setPreselection(String config);

    }

    @Inject
    public ServerConfigPresenter(EventBus eventBus, MyView view, MyProxy proxy, HostInformationStore hostInfoStore,
            ServerGroupStore serverGroupStore, DispatchAsync dispatcher, ApplicationMetaData propertyMetaData,
            BeanFactory factory, PlaceManager placeManager, DomainEntityManager domainManager) {

        super(eventBus, view, proxy);

        this.hostInfoStore = hostInfoStore;
        this.serverGroupStore = serverGroupStore;
        this.dispatcher = dispatcher;
        this.propertyMetaData = propertyMetaData;
        this.factory = factory;
        this.placeManager = placeManager;
        this.domainManager = domainManager;
        this.loadSocketCmd = new LoadSocketBindingsCmd(dispatcher, factory, propertyMetaData);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(ServerWizardEvent.TYPE, this);
        getEventBus().addHandler(HostSelectionChanged.TYPE, this);
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        String action = request.getParameter("action", null);
        if ("new".equals(action)) {
            launchNewConfigDialoge();
        }
        getView().setPreselection(request.getParameter("config", null));
    }

    @Override
    protected void onReset() {
        super.onReset();

        // step1
        if (placeManager.getCurrentPlaceRequest().getNameToken().equals(getProxy().getNameToken())) {
            loadSocketBindings();
        }
    }

    @Override
    public void onHostSelectionChanged() {
        onReset();
    }

    public void onServerConfigSelectionChanged(final Server server) {
        if (server != null) {
            loadJVMConfiguration(server);
            loadProperties(server);
            loadPorts(server);

            SecurityContextChangedEvent
                    .fire(this, "/{selected.host}/server-config=*", server.getName());
        }
    }

    private void loadSocketBindings() {
        serverGroupStore.loadSocketBindingGroupNames(new SimpleCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> result) {
                getView().updateSocketBindings(result);

                // step2
                loadServerConfigurations();
            }
        });
    }

    private void loadServerConfigurations() {
        domainManager.getHosts(new SimpleCallback<HostList>() {
            @Override
            public void onSuccess(HostList hostList) {
                domainManager.getServerConfigurations(hostList.getSelectedHost().getName(),
                        new SimpleCallback<ServerConfigList>() {
                            @Override
                            public void onSuccess(ServerConfigList serverList) {
                                getView().setConfigurations(serverList);
                            }
                        });
            }
        });

        serverGroupStore.loadServerGroups(new SimpleCallback<List<ServerGroupRecord>>() {
            @Override
            public void onSuccess(List<ServerGroupRecord> result) {
                getView().setGroups(result);
            }
        });
    }


    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, HostMgmtPresenter.TYPE_MainContent, this);
    }

    public void launchNewConfigDialoge() {

        window = new DefaultWindow(Console.MESSAGES.createTitle("Server Configuration"));
        window.setWidth(480);
        window.setHeight(360);


        serverGroupStore.loadServerGroups(new SimpleCallback<List<ServerGroupRecord>>() {
            @Override
            public void onSuccess(List<ServerGroupRecord> result) {
                serverGroups = result;
                window.trapWidget(
                        new NewServerConfigWizard(ServerConfigPresenter.this, serverGroups).asWidget()
                );

                window.setGlassEnabled(true);
                window.center();
            }
        });

    }

    public void closeDialoge() {
        if (window != null && window.isShowing()) {
            window.hide();
        }
    }

    public void createServerConfig(final Server newServer) {

        // close popup
        closeDialoge();
        final String newServerName = newServer.getName();


        hostInfoStore.createServerConfig(getSelectedHost(), newServer, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean wasSuccessful) {
                if (wasSuccessful) {

                    Console.info(Console.MESSAGES.added("Server Configuration ") + newServer.getName());

                    loadServerConfigurations();

                } else {
                    closeDialoge();
                    Console.error(Console.MESSAGES.addingFailed("Server Configuration ") + newServer.getName());

                }

                staleModel();

            }

            @Override
            public void onFailure(Throwable caught) {

                Console.getMessageCenter().notify(
                        new Message(Console.MESSAGES.addingFailed("Server Configuration ") + newServer.getName(),
                                Message.Severity.Error)
                );

            }
        });

    }

    private void staleModel() {
        fireEvent(new StaleModelEvent(StaleModelEvent.SERVER_CONFIGURATIONS));
    }

    public void onSaveChanges(final Server entity, Map<String, Object> changedValues) {

        //System.out.println(changedValues);

        if (changedValues.containsKey("portOffset")) { changedValues.put("socketBinding", entity.getSocketBinding()); }

        if (changedValues.containsKey("socketBinding")) { changedValues.put("portOffset", entity.getPortOffset()); }

        final String name = entity.getName();

        ModelNode proto = new ModelNode();
        proto.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        proto.get(ADDRESS).add("host", domainManager.getSelectedHost());
        proto.get(ADDRESS).add(ModelDescriptionConstants.SERVER_CONFIG, name);

        List<PropertyBinding> bindings = propertyMetaData.getBindingsForType(Server.class);
        ModelNode operation = ModelAdapter.detypedFromChangeset(proto, changedValues, bindings);

        // TODO: https://issues.jboss.org/browse/AS7-3643

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Server Configuration ") + name,
                            response.getFailureDescription());

                } else {
                    Console.info(Console.MESSAGES.modified("Server Configuration ") + name);
                }

                loadServerConfigurations();
            }
        });

    }


    public void tryDelete(final Server server) {

        // check if instance exist
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).add("host", domainManager.getSelectedHost());
        operation.get(ADDRESS).add("server", server.getName());
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP).set(READ_RESOURCE_OPERATION);

        System.out.println(operation);
        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable throwable) {
                performDeleteOperation(server); // TODO: really? Upon failure?
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                System.out.println(response);
                String outcome = response.get(OUTCOME).asString();

                Boolean serverIsRunning = outcome.equals(SUCCESS) ? Boolean.TRUE : Boolean.FALSE; // 1.5.x

                // 2.0.x
                if (outcome.equals(SUCCESS)) {
                    serverIsRunning = response.get(RESULT).get("server-state").asString().equalsIgnoreCase("running");
                }

                if (!serverIsRunning) { performDeleteOperation(server); } else {
                    Console.error(
                            Console.MESSAGES.deletionFailed("Server Configuration"),
                            Console.MESSAGES.server_config_stillRunning(server.getName())
                    );
                }
            }
        });


    }

    private void performDeleteOperation(final Server server) {

        hostInfoStore.deleteServerConfig(domainManager.getSelectedHost(), server, new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.getMessageCenter().notify(
                        new Message(Console.MESSAGES.deletionFailed("Server Configuration ") + server.getName(),
                                Message.Severity.Error)
                );
            }

            @Override
            public void onSuccess(Boolean wasSuccessful) {
                if (wasSuccessful) {
                    Console.info(Console.MESSAGES.deleted("Server Configuration ") + server.getName());

                    loadServerConfigurations();
                } else {
                    Console.error(Console.MESSAGES.deletionFailed("Server Configuration ") + server.getName());
                }

                staleModel();
            }
        });
    }

    public String getSelectedHost() {
        return domainManager.getSelectedHost();
    }


    @Override
    public void onCreateJvm(String reference, Jvm jvm) {
        ModelNode address = new ModelNode();
        address.add("host", domainManager.getSelectedHost());
        address.add("server-config", reference);
        address.add(JVM, jvm.getName());
        final String selectedConfigName = reference;

        CreateJvmCmd cmd = new CreateJvmCmd(dispatcher, factory, address);
        cmd.execute(jvm, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadServerConfigurations();
            }
        });
    }

    @Override
    public void onDeleteJvm(String reference, Jvm jvm) {

        ModelNode address = new ModelNode();
        address.add("host", domainManager.getSelectedHost());
        address.add("server-config", reference);
        address.add(JVM, jvm.getName());
        final String selectedConfigName = reference;

        DeleteJvmCmd cmd = new DeleteJvmCmd(dispatcher, factory, address);
        cmd.execute(new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadServerConfigurations();
            }
        });

    }

    @Override
    public void onUpdateJvm(String reference, String jvmName, Map<String, Object> changedValues) {

        if (changedValues.size() > 0) {
            ModelNode address = new ModelNode();
            address.add("host", domainManager.getSelectedHost());
            address.add("server-config", reference);
            address.add(JVM, jvmName);
            final String selectedConfigName = reference;

            UpdateJvmCmd cmd = new UpdateJvmCmd(dispatcher, factory, propertyMetaData, address);
            cmd.execute(changedValues, new SimpleCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    loadServerConfigurations();
                }
            });
        }
    }

    @Override
    public void onCreateProperty(String reference, final PropertyRecord prop) {
        if (propertyWindow != null && propertyWindow.isShowing()) {
            propertyWindow.hide();
        }

        ModelNode address = new ModelNode();
        address.add("host", domainManager.getSelectedHost());
        address.add("server-config", reference);
        address.add("system-property", prop.getKey());
        final String selectedConfigName = reference;

        CreatePropertyCmd cmd = new CreatePropertyCmd(dispatcher, factory, address);
        cmd.execute(prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadServerConfigurations();
            }
        });
    }

    @Override
    public void onDeleteProperty(String reference, final PropertyRecord prop) {

        ModelNode address = new ModelNode();
        address.add("host", domainManager.getSelectedHost());
        address.add("server-config", reference);
        address.add("system-property", prop.getKey());
        final String selectedConfigName = reference;

        DeletePropertyCmd cmd = new DeletePropertyCmd(dispatcher, factory, address);
        cmd.execute(prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadServerConfigurations();
            }
        });
    }

    @Override
    public void onChangeProperty(String reference, PropertyRecord prop) {
        // do nothing
    }

    @Override
    public void launchNewPropertyDialoge(String reference) {
        propertyWindow = new DefaultWindow(Console.MESSAGES.createTitle("System Property"));
        propertyWindow.setWidth(480);
        propertyWindow.setHeight(360);

        propertyWindow.trapWidget(
                new NewPropertyWizard(this, reference, true).asWidget()
        );

        propertyWindow.setGlassEnabled(true);
        propertyWindow.center();
    }

    @Override
    public void closePropertyDialoge() {
        propertyWindow.hide();
    }

    public void loadPorts(final Server server) {

        if (server.getSocketBinding() != null &&
                !server.getSocketBinding().equals("")) {

            loadSocketCmd.execute(server.getSocketBinding(),
                    new SimpleCallback<List<SocketBinding>>() {
                        @Override
                        public void onSuccess(List<SocketBinding> result) {

                            getView().setPorts(server.getSocketBinding(), server, result);
                        }
                    }
            );
        }

    }

    @Override
    public void launchWizard(String HostName) {
        launchNewConfigDialoge();
    }

    public void loadJVMConfiguration(final Server server) {
        hostInfoStore.loadJVMConfiguration(domainManager.getSelectedHost(), server, new SimpleCallback<Jvm>() {
            @Override
            public void onSuccess(Jvm jvm) {
            getView().setJvm(server.getName(), jvm);
            }
        });
    }

    public void loadProperties(final Server server) {
        hostInfoStore
                .loadProperties(domainManager.getSelectedHost(), server, new SimpleCallback<List<PropertyRecord>>() {
                    @Override
                    public void onSuccess(List<PropertyRecord> properties) {
                    getView().setProperties(server.getName(), properties);
                    }
                });
    }

    public void onLaunchCopyWizard(final Server orig) {

        window = new DefaultWindow("New Server Configuration");
        window.setWidth(480);
        window.setHeight(380);


        hostInfoStore.getHosts(new SimpleCallback<List<Host>>() {
            @Override
            public void onSuccess(List<Host> result) {

                window.trapWidget(
                        new CopyServerWizard(ServerConfigPresenter.this, orig, result, domainManager.getSelectedHost())
                                .asWidget()
                );

                window.setGlassEnabled(true);
                window.center();
            }
        });

    }

    public void onSaveCopy(final String targetHost, final Server original, final Server newServer) {
        window.hide();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).setEmptyList();
        operation.get(ADDRESS).add("host", domainManager.getSelectedHost());
        operation.get(ADDRESS).add("server-config", original.getName());
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation, false), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to read server-config: " + original.getName(), caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error("Failed to read server-config: " + original.getName(),
                            response.getFailureDescription());
                } else {
                    ModelNode model = response.get("result").asObject();

                    // required attribute changes: portOffset & serverGroup
                    model.get("socket-binding-port-offset").set(newServer.getPortOffset());
                    model.remove("name");

                    // re-create node

                    ModelNode compositeOp = new ModelNode();
                    compositeOp.get(OP).set(COMPOSITE);
                    compositeOp.get(ADDRESS).setEmptyList();

                    List<ModelNode> steps = new ArrayList<ModelNode>();

                    final ModelNode rootResourceOp = new ModelNode();
                    rootResourceOp.get(OP).set(ADD);
                    rootResourceOp.get(ADDRESS).add("host", targetHost);
                    rootResourceOp.get(ADDRESS).add("server-config", newServer.getName());

                    steps.add(rootResourceOp);

                    DMRUtil.copyResourceValues(model, rootResourceOp, steps);

                    compositeOp.get(STEPS).set(steps);

                    dispatcher.execute(new DMRAction(compositeOp), new SimpleCallback<DMRResponse>() {
                        @Override
                        public void onSuccess(DMRResponse dmrResponse) {
                            ModelNode response = dmrResponse.get();

                            if (response.isFailure()) {
                                Console.error("Failed to copy server-config", response.getFailureDescription());
                            } else {
                                Console.info("Successfully copied server-config '" + newServer.getName() + "'");
                            }

                            loadServerConfigurations();

                        }
                    });

                }

            }

        });
    }
}