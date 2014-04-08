package org.jboss.as.console.client.standalone.runtime;

import java.util.Collections;
import java.util.List;

import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.poc.POC;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.model.SubsystemStore;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.errai.ioc.client.container.IOC;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;

/**
 * @author Heiko Braun
 */
public class StandaloneRuntimePresenter
        extends PerspectivePresenter<StandaloneRuntimePresenter.MyView, StandaloneRuntimePresenter.MyProxy> {

    @NoGatekeeper
    @ProxyCodeSplit
    @NameToken(NameTokens.StandaloneRuntimePresenter)
    public interface MyProxy extends Proxy<StandaloneRuntimePresenter>, Place {}

    public interface MyView extends View {
        void setPresenter(StandaloneRuntimePresenter presenter);
        void setSubsystems(List<SubsystemRecord> result);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();

    private final SubsystemStore subsysStore;

    @Inject
    public StandaloneRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, @POC PlaceManager placeManager,
            SubsystemStore subsysStore, Header header) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.StandaloneRuntimePresenter, TYPE_MainContent);

        this.subsysStore = subsysStore;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(UnauthorizedEvent.TYPE, this);
    }

    @Override
    protected void onReset() {
        subsysStore.loadSubsystems("default", new SimpleCallback<List<SubsystemRecord>>() {
            @Override
            public void onSuccess(List<SubsystemRecord> result) {
                getView().setSubsystems(result);
                StandaloneRuntimePresenter.super.onReset();
            }
        });
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, PlaceManager placeManager, boolean revealDefault) {
        if(revealDefault)
        {
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.StandaloneServerPresenter).build());
        }
    }

    @Override
    protected void revealInParent() {
        org.uberfire.client.mvp.PlaceManager ufPlaceManager = IOC.getBeanManager().lookupBean(org.uberfire.client.mvp.PlaceManager.class).getInstance();
        //ufPlaceManager.goTo("StandaloneRuntime");
        ufPlaceManager.goTo(new DefaultPlaceRequest("StandaloneRuntime", Collections.EMPTY_MAP, false));
    }
}
