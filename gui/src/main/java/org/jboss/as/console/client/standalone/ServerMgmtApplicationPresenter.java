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

package org.jboss.as.console.client.standalone;

import java.util.List;

import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.shared.SubsystemMetaData;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.model.SubsystemStore;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.errai.ioc.client.container.IOC;

/**
 * A collection of tools to manage a standalone server instance.
 *
 * @author Heiko Braun
 */
public class ServerMgmtApplicationPresenter extends
        PerspectivePresenter<ServerMgmtApplicationPresenter.ServerManagementView, ServerMgmtApplicationPresenter.ServerManagementProxy> {

    @NoGatekeeper
    @ProxyCodeSplit
    @NameToken(NameTokens.serverConfig)
    public interface ServerManagementProxy extends ProxyPlace<ServerMgmtApplicationPresenter> {}

    public interface ServerManagementView extends View {
        void updateFrom(List<SubsystemRecord> subsystemRecords);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();

    private PlaceManager placeManager;
    private SubsystemStore subsysStore;

    @Inject
    public ServerMgmtApplicationPresenter(EventBus eventBus, ServerManagementView view,
            ServerManagementProxy proxy, PlaceManager placeManager, SubsystemStore subsysStore, Header header,
            UnauthorisedPresenter unauthorisedPresenter) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.serverConfig, unauthorisedPresenter,
                TYPE_MainContent);

        this.placeManager = placeManager;
        this.subsysStore = subsysStore;
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest) {
        subsysStore.loadSubsystems("default", new SimpleCallback<List<SubsystemRecord>>() {
            @Override
            public void onSuccess(List<SubsystemRecord> existingSubsystems) {
                getView().updateFrom(existingSubsystems);

                // chose default view if necessary
                PlaceRequest preference = NameTokens.serverConfig
                        .equals(placeRequest.getNameToken()) ? preferredPlace() : placeRequest;

                final String[] defaultSubsystem = SubsystemMetaData
                        .getDefaultSubsystem(preference.getNameToken(), existingSubsystems);
                placeManager.revealPlace(new PlaceRequest.Builder().nameToken(defaultSubsystem[1]).build());
            }
        });
    }

    @Override
    protected void onDefaultPlace(final PlaceManager placeManager) {
        onFirstReveal(preferredPlace());
    }

    @Override
    protected void revealInParent() {
        //RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
        org.uberfire.client.mvp.PlaceManager ufPlaceManager = IOC.getBeanManager().lookupBean(org.uberfire.client.mvp.PlaceManager.class).getInstance();
        ufPlaceManager.goTo("Profile");
    }

    private PlaceRequest preferredPlace() {
        return new PlaceRequest.Builder().nameToken(NameTokens.DataSourcePresenter).build();
    }
}
