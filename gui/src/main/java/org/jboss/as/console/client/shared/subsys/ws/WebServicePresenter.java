package org.jboss.as.console.client.shared.subsys.ws;

import static org.jboss.dmr.client.ModelDescriptionConstants.OP;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

import java.util.Map;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.poc.POC;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.messaging.model.MessagingProvider;
import org.jboss.as.console.client.shared.subsys.ws.model.WebServiceProvider;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

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
 * @date 6/10/11
 */
public class WebServicePresenter extends Presenter<WebServicePresenter.MyView, WebServicePresenter.MyProxy> {

    private final PlaceManager placeManager;
    private final DispatchAsync dispatcher;
    private final BeanFactory factory;
    private MessagingProvider providerEntity;
    private final DefaultWindow window = null;
    private final RevealStrategy revealStrategy;
    private final ApplicationMetaData propertyMetaData;

    private final EntityAdapter<WebServiceProvider> providerAdapter;
    private final BeanMetaData beanMeta;

    @ProxyCodeSplit
    @NameToken(NameTokens.WebServicePresenter)
    @AccessControl(resources = {
            "{selected.profile}/subsystem=webservices"

    })
    public interface MyProxy extends Proxy<WebServicePresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(WebServicePresenter presenter);
        void setProvider(WebServiceProvider webServiceProvider);
    }

    @Inject
    public WebServicePresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            @POC PlaceManager placeManager,DispatchAsync dispatcher,
            BeanFactory factory, RevealStrategy revealStrategy,
            ApplicationMetaData metaData, EndpointRegistry registry) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.revealStrategy = revealStrategy;
        this.propertyMetaData = metaData;


        providerAdapter = new EntityAdapter<WebServiceProvider>(
                WebServiceProvider.class, metaData
        );

        beanMeta = metaData.getBeanMetaData(WebServiceProvider.class);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }


    @Override
    protected void onReset() {
        super.onReset();

        loadProvider();
    }

    private void loadProvider() {
        ModelNode operation = beanMeta.getAddress().asResource(
                Baseadress.get()
        );

        operation.get(OP).set(READ_RESOURCE_OPERATION);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Console.error(Console.MESSAGES.failed("Loading Web Service Provider"), response.getFailureDescription());
                }
                else
                {
                    WebServiceProvider webServiceProvider = providerAdapter.fromDMR(response.get(RESULT));
                    getView().setProvider(webServiceProvider);
                }
            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public void onSaveProvider(Map<String, Object> changes) {
        ModelNode address = beanMeta.getAddress().asResource(
                Baseadress.get()
        );

        ModelNode operation = providerAdapter.fromChangeset(changes, address);


        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                //System.out.println(response);
                if(response.isFailure())
                {
                    Console.error(Console.MESSAGES.modificationFailed("Web Service Provider"), response.getFailureDescription());
                }
                else
                {
                    Console.info(Console.MESSAGES.modified("Web Service Provider"));
                    loadProvider();
                }
            }
        });
    }
}
