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

package org.jboss.as.console.client.standalone.runtime;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;

import org.jboss.as.console.client.Console;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.lifecycle.OnOpen;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.gwtplatform.mvp.client.PresenterWidget;

@Dependent
@WorkbenchScreen(identifier = "StandaloneRuntimePresenterScreen")
public class StandaloneRuntimePresenterScreen {

  private static StandaloneRuntimePresenter gwtpPresenter;

  @PostConstruct
  public void initGWTPPresenter() {
      Console.MODULES.getRuntimePresenter().get(new AsyncCallback<StandaloneRuntimePresenter>() {
          @Override
          public void onFailure(Throwable caught) {
              throw new RuntimeException(caught);
          }

          @Override
          public void onSuccess(StandaloneRuntimePresenter result) {
              gwtpPresenter = result;
          }
      });
  }

  @WorkbenchPartTitle
  public String getTitle() {
    return Console.CONSTANTS.common_label_runtimeName();
  }

  @WorkbenchPartView
  public IsWidget getView() {
      return gwtpPresenter;
  }

  @OnOpen
  public void onOpen() {
      
      // workaround: we need to remove the relative positioning from this
      // element and its parent (which is a private internal element of
      // ScrollPanel). Fortunately this is just temporary while we're
      // incrementally switching over to UberFire + Errai
      Element presenterElement = gwtpPresenter.asWidget().getElement();
      presenterElement.getStyle().clearPosition();
      presenterElement.getParentElement().getStyle().clearPosition();
      
      if (!gwtpPresenter.isVisible()) {
          forceInternalReveal(gwtpPresenter);
      }
  }

  private static native void forceInternalReveal(PresenterWidget<?> presenter) /*-{
      presenter.@com.gwtplatform.mvp.client.PresenterWidget::internalReveal()();
  }-*/;
}
