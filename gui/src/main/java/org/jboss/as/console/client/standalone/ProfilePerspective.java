package org.jboss.as.console.client.standalone;

import org.jboss.as.console.client.core.NameTokens;
import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PanelType;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PartDefinitionImpl;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;

/**
 * An UberFire perspective that houses the
 * {@link ServerMgmtApplicationPresenter}, which is the GWTP component providing
 * the "Profile" UI in the console.
 * <p>
 * Future plans: once all GWTP components that take up the
 * MainLayoutPresenter.TYPE_MainContent slot have been converted, these UberFire
 * perspectives can manage their own views in the split panels, and all child
 * GWTP components can be converted to UberFire WorkbenchScreens or
 * WorkbenchEditors. This work can be done in batches, one perspective at a time.
 *
 * @author jfuerth
 */
@WorkbenchPerspective(identifier = "Profile", isDefault = true)
public class ProfilePerspective {

    @Perspective
    public PerspectiveDefinition getPerspectiveDefinition() {
        final PerspectiveDefinition p = new PerspectiveDefinitionImpl(PanelType.ROOT_LIST); // TODO use ROOT_SIMPLE instead?
        p.setTransient(true);
        p.setName(NameTokens.serverConfig);

        p.getRoot().addPart(
            new PartDefinitionImpl(
                    new DefaultPlaceRequest("ProfilePresenterScreen")));
        return p;
    }

}
