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

package org.jboss.as.console.client.core;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.poc.POC;
import org.jboss.as.console.client.rbac.ReadOnlyContext;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.proxy.PlaceManagerImpl;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.TokenFormatter;

/**
 * @author Heiko Braun
 * @date 2/4/11
 */
@ApplicationScoped
public class DefaultPlaceManager extends PlaceManagerImpl {

    private final SecurityFramework securityFramework;
    private final BootstrapContext bootstrap;
    private final EventBus eventBus;

    @Inject
    public DefaultPlaceManager(EventBus eventBus, @POC TokenFormatter tokenFormatter, BootstrapContext bootstrap,
            SecurityFramework securityManager) {
        super(eventBus, tokenFormatter);
        this.bootstrap = bootstrap;
        this.eventBus = eventBus;
        this.securityFramework = securityManager;
    }

    @Override
    public void revealErrorPlace(String invalidHistoryToken) {

        revealDefaultPlace();
    }

    @Override
    public void revealDefaultPlace() {

        List<PlaceRequest> places = new ArrayList<PlaceRequest>();
        places.add(bootstrap.getDefaultPlace());

        revealPlaceHierarchy(places);
    }

    final class ContextCreation {
        final PlaceRequest request;
        Throwable error;

        ContextCreation(PlaceRequest request) {
            this.request = request;
        }

        PlaceRequest getRequest() {
            return request;
        }

        Throwable getError() {
            return error;
        }

        void setError(Throwable error) {
            this.error = error;
        }
    }

    @Override
    protected void doRevealPlace(final PlaceRequest request, final boolean updateBrowserUrl) {
        Function<ContextCreation> createContext = new Function<ContextCreation>() {
            @Override
            public void execute(final Control<ContextCreation> control) {
                final String nameToken = control.getContext().getRequest().getNameToken();
                final SecurityContext context = securityFramework.getSecurityContext(nameToken);
                if (context == null || (context instanceof ReadOnlyContext)) {
                    // force re-creation if read-only fallback
                    securityFramework.createSecurityContext(nameToken, new AsyncCallback<SecurityContext>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            control.getContext().setError(throwable);
                            control.abort();
                        }

                        @Override
                        public void onSuccess(SecurityContext securityContext) {
                            control.proceed();
                        }
                    });
                } else {
                    control.proceed();
                }
            }
        };

        Outcome<ContextCreation> outcome = new Outcome<ContextCreation>() {
                    @Override
                    public void onFailure(ContextCreation context) {
                        unlock();
                        //noinspection ThrowableResultOfMethodCallIgnored
                        Console.error("Failed to create security context", context.getError().getMessage());
                    }

                    @Override
                    public void onSuccess(final ContextCreation context) {
                        // unlock(); // remove?
                        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {
                                final PlaceRequest placeRequest = context.getRequest();
                                DefaultPlaceManager.super.doRevealPlace(placeRequest, updateBrowserUrl);
                                StringBuffer nameToken = new StringBuffer(placeRequest.getNameToken());

                                if(!placeRequest.getParameterNames().isEmpty())
                                {
                                    nameToken.append(";");
                                    for(String param : placeRequest.getParameterNames())
                                    {
                                        nameToken.append(param).append("=").append(placeRequest.getParameter(param,""));
                                    }
                                }
                                eventBus.fireEvent(new LHSHighlightEvent(nameToken.toString()));
                            }
                        });
                    }
                };

        new Async<ContextCreation>(Footer.PROGRESS_ELEMENT)
                .waterfall(new ContextCreation(request), outcome, createContext);
    }

    @Override
    public void revealUnauthorizedPlace(String unauthorizedHistoryToken) {
        // Update the history token for the user to see the unauthorized token, but don't navigate!
        updateHistory(new PlaceRequest.Builder().nameToken(unauthorizedHistoryToken).build(), true);
        // Send an unauthorized event notifying the top level presenters to show
        // the unauthorized presenter widget in the main content slot
        UnauthorizedEvent.fire(this, unauthorizedHistoryToken);
    }
}
