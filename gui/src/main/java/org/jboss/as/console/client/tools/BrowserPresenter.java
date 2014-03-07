package org.jboss.as.console.client.tools;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.dmr.client.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.dmr.client.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.dmr.client.ModelDescriptionConstants.OP;
import static org.jboss.dmr.client.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;
import static org.jboss.dmr.client.ModelDescriptionConstants.STEPS;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.poc.POC;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PopupView;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.proxy.PlaceManager;

/**
 * @author Heiko Braun
 * @date 6/15/12
 */
public class BrowserPresenter extends PresenterWidget<BrowserPresenter.MyView>{

    private final PlaceManager placeManager;
    private final DispatchAsync dispatcher;
    private boolean hasBeenRevealed;
    private DefaultWindow window;

    public interface MyView extends PopupView {
        void setPresenter(BrowserPresenter presenter);
        void updateChildrenTypes(ModelNode address, List<ModelNode> modelNodes);
        void updateChildrenNames(ModelNode address, List<ModelNode> modelNodes);
        void updateResource(ModelNode address, ModelNode resource);
        void updateDescription(ModelNode address, ModelNode description);
    }

    @Inject
    public BrowserPresenter(
            EventBus eventBus, MyView view,
            @POC PlaceManager placeManager, DispatchAsync dispatcher) {
        super(eventBus, view);

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReveal() {
        if(!hasBeenRevealed)
        {
            hasBeenRevealed = true;
            onRefresh();
        }
    }

    @Override
    protected void onHide() {

    }

    public void readChildrenTypes(final ModelNode address) {

        ModelNode operation  = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(READ_CHILDREN_TYPES_OPERATION);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                final ModelNode response = dmrResponse.get();
                getView().updateChildrenTypes(address, response.get(RESULT).asList());
            }
        });
    }

    public void readChildrenNames(final ModelNode address) {

        final List<ModelNode> addressList = address.asList();
        ModelNode typeDenominator = null;
        List<ModelNode> actualAddress = new ArrayList<ModelNode>();
        int i=0;
        for(ModelNode path : addressList)
        {
            if(i<addressList.size()-1)
                actualAddress.add(path);
            else
                typeDenominator = path;

            i++;
        }

        ModelNode operation  = new ModelNode();
        operation.get(ADDRESS).set(actualAddress);
        operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        operation.get(CHILD_TYPE).set(typeDenominator.asProperty().getName());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                final ModelNode response = dmrResponse.get();
                getView().updateChildrenNames(address, response.get(RESULT).asList());
            }
        });

    }

    public void readResource(final ModelNode address) {

        ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();


        List<ModelNode> steps = new ArrayList<ModelNode>();

        // the description
        ModelNode descriptionOp  = new ModelNode();
        descriptionOp.get(ADDRESS).set(address);
        descriptionOp.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        descriptionOp.get(OPERATIONS).set(true);
        steps.add(descriptionOp);

        // the actual values
        final ModelNode resourceOp  = new ModelNode();
        resourceOp.get(ADDRESS).set(address);
        resourceOp.get(OP).set(READ_RESOURCE_OPERATION);
        resourceOp.get(INCLUDE_RUNTIME).set(true);
        steps.add(resourceOp);

        operation.get(STEPS).set(steps);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {

                final ModelNode response = dmrResponse.get();
                List<Property> propertyList = response.get(RESULT).asPropertyList();

                for(Property step : propertyList)
                {
                    ModelNode stepResult = step.getValue();
                    if(step.getName().equals("step-1"))
                    {
                        ModelNode desc = null;
                        if(ModelType.LIST.equals(stepResult.get(RESULT).getType()))
                            desc = stepResult.get(RESULT).asList().get(0).get(RESULT).asObject();
                        else
                        {
                            // workaround ...
                            if(!stepResult.hasDefined(RESULT))
                            {
                                Log.error("Undefined element: "+address);
                                desc = new ModelNode();
                            }
                            else
                            {
                                desc = stepResult.get(RESULT).asObject();
                            }
                        }

                        getView().updateDescription(address, desc);
                    }
                    else if(step.getName().equals("step-2"))
                    {
                        if(!stepResult.isFailure())
                            getView().updateResource(address, stepResult.get(RESULT).asObject());
                        else
                            Console.warning("Failed to read "+ resourceOp.get(ADDRESS));
                    }
                }
            }}
        );
    }

    public void onRefresh() {
        readChildrenTypes(new ModelNode().setEmptyList());

    }

    // ---------- Storage Presenter  ----

    /*public void loadDmrDescription(ModelNode address, final AsyncCallback<ModelNode> callback) {
        ModelNode descriptionOp  = new ModelNode();
        descriptionOp.get(ADDRESS).set(address);
        descriptionOp.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        descriptionOp.get(OPERATIONS).set(true);

        dispatcher.execute(new DMRAction(descriptionOp), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {

                final ModelNode response = dmrResponse.get();

                if(response.isFailure())
                {
                    Console.error("Failed to load DMR description", response.getFailureDescription());
                }
                else
                {
                    final ModelNode result = response.get(RESULT);
                    ModelNode actualDescriptionNode = null;
                    if(ModelType.LIST==result.getType()) // in case of wildcard requests
                    {
                        final ModelNode node = result.asList().get(0).asObject();
                        actualDescriptionNode = node.get(RESULT).asObject();
                    }
                    else
                    {
                        actualDescriptionNode = result;
                    }

                    callback.onSuccess(actualDescriptionNode);
                }
            }}
        );
    }

    public void loadResourceData(ModelNode address, final AsyncCallback<ModelNode> callback)
    {
        ModelNode resourceOp  = new ModelNode();
        resourceOp.get(ADDRESS).set(address);
        resourceOp.get(OP).set(READ_RESOURCE_OPERATION);

        dispatcher.execute(new DMRAction(resourceOp), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {

                final ModelNode response = dmrResponse.get();

                if(response.isFailure())
                {
                    Console.error("Failed to load resource data", response.getFailureDescription());
                }
                else
                {
                    final ModelNode result = response.get(RESULT).asObject();
                    callback.onSuccess(result);
                }
            }}
        );
    }        */
}
