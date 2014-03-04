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
package org.jboss.as.console.client.poc;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.errai.common.client.api.Assert;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.lifecycle.OnClose;
import org.uberfire.lifecycle.OnOpen;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.ResetPresentersEvent;
import com.gwtplatform.mvp.client.proxy.ResetPresentersHandler;

public abstract class BasePerspectivePresenterScreen implements ResetPresentersHandler {

    /**
     * The presenter that we're managing. Gets set by subclasses calling into setGwtpPresenter().
     */
    private PerspectivePresenter gwtpPresenter;
    
    /**
     * Our registration handle for ResetPresentersEvent. Is set to null when we
     * are not currently registered for that event.
     */
    private HandlerRegistration handlerRegistration;
    
    public void setGwtpPresenter(PerspectivePresenter gwtpPresenter) {
        this.gwtpPresenter = Assert.notNull(gwtpPresenter);
    }
    
    @WorkbenchPartView
    public IsWidget getView() {
        return Assert.notNull(gwtpPresenter);
    }

    @OnOpen
    public void onOpen() {
        
        handlerRegistration = Console.MODULES.getEventBus().addHandler(ResetPresentersEvent.getType(), this);

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

    @OnClose
    public void onClose() {
        try {
            if (gwtpPresenter.isVisible()) {
                forceInternalHide(gwtpPresenter);
            }
        } finally {
            handlerRegistration.removeHandler();
            handlerRegistration = null;
        }
    }

    private static native void forceInternalHide(PresenterWidget<?> presenter) /*-{
        presenter.@com.gwtplatform.mvp.client.PresenterWidget::internalHide()();
    }-*/;

    @Override
    public void onResetPresenters(ResetPresentersEvent resetPresentersEvent) {
        forceInternalReset(gwtpPresenter);
    }
    
    private static native void forceInternalReset(PresenterWidget<?> presenter) /*-{
        presenter.@com.gwtplatform.mvp.client.PresenterWidget::internalReset()();
    }-*/;
}
