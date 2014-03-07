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

package org.jboss.as.console.client.administration.role.ui;

import static org.jboss.as.console.client.administration.role.model.Principal.Type.GROUP;
import static org.jboss.as.console.client.administration.role.model.Principal.Type.USER;

import java.util.List;

import javax.enterprise.context.Dependent;

import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.model.Principals;
import org.jboss.as.console.client.administration.role.model.RoleAssignments;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * @author Harald Pehl
 */
@Dependent
public class RoleAssignementView implements RoleAssignmentPresenter.MyView {

    private RoleAssignmentPresenter presenter;
    private RoleAssignmentEditor groupEditor;
    private RoleAssignmentEditor userEditor;
    private RoleEditor roleEditor;
    private DefaultTabLayoutPanel widget;

    @Inject
    public RoleAssignementView() {
    }

    @Override
    public void init(RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void update(final Principals principals, RoleAssignments assignments, Roles roles, final List<String> hosts,
            final List<String> serverGroups) {
        groupEditor.update(assignments, roles);
        userEditor.update(assignments, roles);
        roleEditor.update(roles, hosts, serverGroups);
    }

    @Override
    public Widget asWidget() {
        if (widget == null) {
            groupEditor = new RoleAssignmentEditor(presenter, GROUP);
            userEditor = new RoleAssignmentEditor(presenter, USER);
            roleEditor = new RoleEditor(presenter);

            DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
            tabLayoutpanel.addStyleName("default-tabpanel");
            tabLayoutpanel.add(userEditor, "Users", true);
            tabLayoutpanel.add(groupEditor, "Groups", true);
            tabLayoutpanel.add(roleEditor, "Roles", true);
            tabLayoutpanel.selectTab(0);

            widget = tabLayoutpanel;
        }
        return widget;
    }

}
