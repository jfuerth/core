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

package org.jboss.as.console.client.domain.hosts.general;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;

import java.util.List;

import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.hosts.HostMgmtPresenter;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.PlaceRequestSecurityFramework;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.properties.CreatePropertyCmd;
import org.jboss.as.console.client.shared.properties.DeletePropertyCmd;
import org.jboss.as.console.client.shared.properties.LoadPropertiesCmd;
import org.jboss.as.console.client.shared.properties.NewPropertyWizard;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.as.console.client.shared.state.HostSelectionChanged;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;

/**
 * @author Heiko Braun
 * @date 5/17/11
 */
public class HostPropertiesPresenter extends Presenter<HostPropertiesPresenter.MyView, HostPropertiesPresenter.MyProxy>
        implements PropertyManagement, HostSelectionChanged.ChangeListener {

    @ProxyCodeSplit
    @NameToken(NameTokens.HostPropertiesPresenter)
    @OperationMode(DOMAIN)
    @AccessControl(resources = {"/{selected.host}/system-property=*",})
    public interface MyProxy extends Proxy<HostPropertiesPresenter>, Place {}


    public interface MyView extends View {

        void setPresenter(HostPropertiesPresenter presenter);

        void setProperties(List<PropertyRecord> properties);
    }


    private final DispatchAsync dispatcher;
    private final DomainEntityManager domainManager;
    private final BeanFactory factory;
    private final PlaceRequestSecurityFramework placeRequestSecurityFramework;
    private DefaultWindow propertyWindow;


    @Inject
    public HostPropertiesPresenter(EventBus eventBus, MyView view, MyProxy proxy, DispatchAsync dispatcher,
            BeanFactory factory, DomainEntityManager domainManager,
            PlaceRequestSecurityFramework placeRequestSecurityFramework) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.domainManager = domainManager;
        this.factory = factory;
        this.placeRequestSecurityFramework = placeRequestSecurityFramework;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(HostSelectionChanged.TYPE, this);
        placeRequestSecurityFramework.addCurrentContext(hostPlaceRequest());
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadProperties();
    }

    @Override
    public void onHostSelectionChanged() {
        if (isVisible()) {
            placeRequestSecurityFramework.update(this, hostPlaceRequest());
            loadProperties();
        }
    }

    private PlaceRequest hostPlaceRequest() {
        return new PlaceRequest.Builder().nameToken(getProxy().getNameToken())
                .with("host", domainManager.getSelectedHost()).build();
    }

    private void loadProperties() {
        ModelNode address = new ModelNode();
        address.add("host", domainManager.getSelectedHost());
        LoadPropertiesCmd loadPropCmd = new LoadPropertiesCmd(dispatcher, factory, address);
        loadPropCmd.execute(new SimpleCallback<List<PropertyRecord>>() {
            @Override
            public void onSuccess(List<PropertyRecord> result) {
                getView().setProperties(result);
            }
        });
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, HostMgmtPresenter.TYPE_MainContent, this);
    }

    @Override
    public void closePropertyDialoge() {
        propertyWindow.hide();
    }

    @Override
    public void launchNewPropertyDialoge(String group) {

        propertyWindow = new DefaultWindow("New Host Property");
        propertyWindow.setWidth(480);
        propertyWindow.setHeight(360);
        propertyWindow.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {

            }
        });

        propertyWindow.trapWidget(
                new NewPropertyWizard(this, group, true).asWidget()
        );

        propertyWindow.setGlassEnabled(true);
        propertyWindow.center();
    }

    @Override
    public void onCreateProperty(final String groupName, final PropertyRecord prop) {

        if (propertyWindow != null && propertyWindow.isShowing()) {
            propertyWindow.hide();
        }

        ModelNode address = new ModelNode();
        address.add("host", domainManager.getSelectedHost());
        address.add("system-property", prop.getKey());

        CreatePropertyCmd cmd = new CreatePropertyCmd(dispatcher, factory, address);
        cmd.execute(prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadProperties();
            }
        });


    }

    @Override
    public void onDeleteProperty(final String groupName, final PropertyRecord prop) {
        ModelNode address = new ModelNode();
        address.add("host", domainManager.getSelectedHost());
        address.add("system-property", prop.getKey());

        DeletePropertyCmd cmd = new DeletePropertyCmd(dispatcher, factory, address);
        cmd.execute(prop, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadProperties();
            }
        });
    }

    @Override
    public void onChangeProperty(String groupName, PropertyRecord prop) {
        // do nothing
    }
}
