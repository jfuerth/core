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
package org.jboss.as.console.client.shared.runtime.naming;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.OP;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.poc.POC;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.as.console.client.shared.state.ServerSelectionChanged;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;

/**
 * @author Heiko Braun
 * @date 7/20/11
 */
public class JndiPresenter extends Presenter<JndiPresenter.MyView, JndiPresenter.MyProxy>
        implements ServerSelectionChanged.ChangeListener {

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final BeanFactory factory;
    private final DomainEntityManager domainEntityManager;

    @ProxyCodeSplit
    @NameToken(NameTokens.JndiPresenter)
    @AccessControl(
            resources = {
                    "/{selected.host}/{selected.server}/subsystem=naming"
            },
            operations = {
                    "/{selected.host}/{selected.server}/subsystem=naming#jndi-view"
            },
            recursive = false
    )
    public interface MyProxy extends Proxy<JndiPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(JndiPresenter presenter);
        void setJndiTree(CellTree tree, SingleSelectionModel<JndiEntry> selectionModel);

        void clearValues();
    }

    @Inject
    public JndiPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            @POC PlaceManager placeManager, RevealStrategy revealStrategy,
            DispatchAsync dispatcher, BeanFactory factory,
            DomainEntityManager domainEntityManager) {

        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.domainEntityManager = domainEntityManager;

    }

    @Override
    public void onServerSelectionChanged(boolean isRunning) {
        if(isVisible())
        {
            loadJndiTree();
        }
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(ServerSelectionChanged.TYPE, this);

    }

    @Override
    protected void onReset() {
        super.onReset();
        loadJndiTree();
    }

    private void loadJndiTree() {

        getView().clearValues();

        ModelNode operation = new ModelNode();
        operation.get(OP).set("jndi-view");
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "naming");

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();

                if(result.isFailure())
                {
                    Log.error("Failed to load JNDI: "+ result.getFailureDescription());
                }
                else
                {
                    ModelNode model = result.get(RESULT);

                    CellTree cellTree = null;
                    JndiTreeParser parser = new JndiTreeParser();
                    if(model.hasDefined("java: contexts"))
                        cellTree = parser.parse(model.get("java: contexts").asPropertyList());

                    if(model.hasDefined("applications")) {
                        ModelNode tempParent = new ModelNode();
                        ModelNode apps = model.get("applications");
                        tempParent.get("applications").set(apps);
                        cellTree = parser.parse(tempParent.asPropertyList());
                    }

                    if(cellTree != null)
                        getView().setJndiTree(cellTree, parser.getSelectionModel());

                }
            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }
}
