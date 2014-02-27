/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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

import com.google.gwt.user.client.rpc.AsyncCallback;
import javax.enterprise.context.Dependent;

import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import javax.annotation.PostConstruct;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.AdministrationPresenter;
import org.jboss.as.console.client.core.SuspendableView;
import org.jboss.as.console.client.core.SuspendableViewImpl;

@Dependent
@WorkbenchScreen(identifier = "AdminPresenterScreen")
public class AdminPresenterScreen {

    private static AdministrationPresenter gwtpPresenter;

    @PostConstruct
    public void initGWTPPresenter() {
        Console.MODULES.getAdministrationPresenter().get(new AsyncCallback<AdministrationPresenter>() {
            @Override
            public void onFailure(Throwable caught) {
                throw new RuntimeException(caught);
            }

            @Override
            public void onSuccess(AdministrationPresenter result) {
                gwtpPresenter = result;
            }
        });
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return "Administration";
    }

    @WorkbenchPartView
    public IsWidget getView() {
        System.out.println(">>>> gwtpPresenter=" + gwtpPresenter);
        System.out.println(">>>> presenter=" + gwtpPresenter.getView());
        return gwtpPresenter;
        //return new Label("I got added to the DOM");
    }
}
