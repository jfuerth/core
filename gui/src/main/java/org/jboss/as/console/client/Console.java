
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

package org.jboss.as.console.client;

import java.util.EnumSet;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.as.console.client.core.AsyncCallHandler;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.core.UIDebugConstants;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.core.bootstrap.EagerLoadGroups;
import org.jboss.as.console.client.core.bootstrap.EagerLoadHosts;
import org.jboss.as.console.client.core.bootstrap.EagerLoadProfiles;
import org.jboss.as.console.client.core.bootstrap.ExecutionMode;
import org.jboss.as.console.client.core.bootstrap.InsufficientPrivileges;
import org.jboss.as.console.client.core.bootstrap.LoadCompatMatrix;
import org.jboss.as.console.client.core.bootstrap.LoadGoogleViz;
import org.jboss.as.console.client.core.bootstrap.LoadMainApp;
import org.jboss.as.console.client.core.bootstrap.RegisterSubsystems;
import org.jboss.as.console.client.core.bootstrap.TrackExecutionMode;
import org.jboss.as.console.client.core.gin.Composite;
import org.jboss.as.console.client.core.gin.ErraiSimpleEventBusProvider;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.core.message.MessageCenter;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.poc.POC;
import org.jboss.as.console.client.shared.Preferences;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.state.ReloadNotification;
import org.jboss.as.console.client.shared.state.ReloadState;
import org.jboss.as.console.client.shared.state.ServerState;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.progress.ProgressPolyfill;
import org.jboss.dmr.client.dispatch.DispatchError;
import org.jboss.dmr.client.notify.Notifications;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;
import org.uberfire.client.workbench.Workbench;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.proxy.AsyncCallFailEvent;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TokenFormatter;

/**
 * Main application entry point. Executes several initialisation phases.
 *
 * @author Heiko Braun
 */
@EntryPoint
public class Console implements ReloadNotification.Handler {

    // must wait until PostConstruct before bootstrapping GIN, because it calls
    // back into Errai's BeanManager during initialization
    public static Composite MODULES;

    public final static UIConstants CONSTANTS = GWT.create(UIConstants.class);
    public final static UIDebugConstants DEBUG_CONSTANTS = GWT.create(UIDebugConstants.class);
    public final static UIMessages MESSAGES = GWT.create(UIMessages.class);

    @Produces
    private final ApplicationMetaData applicationMetaData = GWT.create(ApplicationMetaData.class);

    @Inject
    private Workbench workbench;

    @Inject @POC
    private PlaceManager gwtpPlaceManager;

    @Inject @POC
    private TokenFormatter tokenFormatter;

    @Inject
    private BootstrapContext bootstrapContext;

    @Inject @POC
    private ProductConfig prodConfig;

    /**
     * Temporary reference to the Errai-created instance of this class. Remains
     * null until the PostConstruct method has been invoked (by that time, all
     * the injections have been resolved)
     */
    public static Console INSTANCE;

    @PostConstruct
    public void blockWorkbenchStartup() {
        INSTANCE = this;
        workbench.addStartupBlocker(Console.class);
        if (workbench == null) return;
        Log.setUncaughtExceptionHandler();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                onModuleLoad2();
            }
        });
    }

    public void onModuleLoad2() {
        // register global code split call handler
        new ErraiSimpleEventBusProvider().get().addHandler(AsyncCallFailEvent.getType(), new AsyncCallHandler(gwtpPlaceManager));

        // load console css bundle
        ConsoleResources.INSTANCE.css().ensureInjected();

        if(prodConfig.getProfile().equals(ProductConfig.Profile.COMMUNITY))
            ConsoleResources.INSTANCE.communityStyles().ensureInjected();
        else
            ConsoleResources.INSTANCE.productStyles().ensureInjected();

        // inject pretty print resources
        ConsoleResources.INSTANCE.prettifyCss().ensureInjected();
        ScriptInjector.fromString(ConsoleResources.INSTANCE.prettifyJs().getText()).setWindow(ScriptInjector.TOP_WINDOW)
                .inject();

        // Inject progress polyfill js code
        ProgressPolyfill.inject();

        GWT.runAsync(new RunAsyncCallback() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load application components!");
            }

            @Override
            public void onSuccess() {
                //DelayedBindRegistry.bind(Assert.notNull(MODULES));

                // dump prefs
                for(Preferences.Key key : Preferences.Key.values())
                {
                    String prefValue = Preferences.get(key) !=null ? Preferences.get(key) : "n/a";
                    Log.info(key.getTitle()+": "+ prefValue);
                }

                // Bootstrap outcome: Load main application or display error message
                Outcome<BootstrapContext> bootstrapOutcome = new Outcome<BootstrapContext>() {
                    @Override
                    public void onFailure(BootstrapContext context) {

                        hideLoadingPanel();

                        String cause = "";
                        int status = 500;
                        if(context.getLastError()!=null)
                        {
                            cause = context.getLastError().getMessage();
                            if(context.getLastError() instanceof DispatchError)
                            {
                                status = ((DispatchError)context.getLastError()).getStatusCode();
                            }
                        }

                        if(403==status)
                        {
                            // authorisation error (lack of priviledges)
                            new InsufficientPrivileges().execute();
                        }
                        else
                        {
                            // unknown error
                            HTMLPanel explanation = new HTMLPanel("<div style='padding-top:150px;padding-left:120px;'><h2>The management interface could not be loaded.</h2><pre>"+cause+"</pre></div>");
                            RootLayoutPanel.get().add(explanation);
                        }
                    }

                    @Override
                    public void onSuccess(BootstrapContext context) {
                        hideLoadingPanel();

                        // DMR notifications
                        Notifications.addReloadHandler(Console.this);

                        new LoadMainApp(
                                bootstrapContext,
                                gwtpPlaceManager,
                                tokenFormatter).execute();
                    }
                };

                Console.MODULES = GWT.create(Composite.class);

                // Ordered execution: if any of these fail, the interface wil not be loaded
                new Async<BootstrapContext>().waterfall(
                        bootstrapContext, // shared context
                        bootstrapOutcome, // outcome

                        // bootstrap functions
                        // Activate once CORS is supported / Keymaker is in place
                        // new BootstrapServerSetup(),
                        new LoadGoogleViz(),
                        new ExecutionMode(MODULES.getDispatchAsync(), workbench),
                        new TrackExecutionMode(MODULES.getAnalytics()),
                        new LoadCompatMatrix(MODULES.modelVersions()),
                        new RegisterSubsystems(MODULES.getSubsystemRegistry()),
                        new EagerLoadProfiles(MODULES.getProfileStore(), MODULES.getCurrentSelectedProfile()),
                        new EagerLoadHosts(MODULES.getDomainEntityManager()),
                        new EagerLoadGroups(MODULES.getServerGroupStore())
                );
            }
        });
    }

    /**
     * Hides the "Loading" panel that exists in the host HTML page.
     */
    private static void hideLoadingPanel() {
        RootPanel.get("loading-panel").removeFromParent();
    }

    public static void info(String message) {
        getMessageCenter().notify(
                new Message(message, Message.Severity.Info)
        );
    }

    public static void error(String message) {
        getMessageCenter().notify(
                new Message(message, Message.Severity.Error)
        );
    }

    public static void error(String message, String detail) {
        getMessageCenter().notify(
                new Message(message, detail, Message.Severity.Error)
        );
    }

    public static void warning(String message) {
        getMessageCenter().notify(
                new Message(message, Message.Severity.Warning)
        );
    }

    public static void warning(String message, boolean sticky) {
        Message msg = sticky ?
                new Message(message, Message.Severity.Warning, EnumSet.of(Message.Option.Sticky)) :
                new Message(message, Message.Severity.Warning);

        getMessageCenter().notify(msg);
    }

    public static void warning(String message, String detail, boolean sticky) {
        Message msg = sticky ?
                new Message(message, detail, Message.Severity.Warning, EnumSet.of(Message.Option.Sticky)) :
                new Message(message, detail, Message.Severity.Warning);


        getMessageCenter().notify(msg);
    }

    public static void warning(String message, String detail) {
        getMessageCenter().notify(
                new Message(message, detail, Message.Severity.Warning)
        );
    }

    public static void schedule(final Command cmd)
    {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                cmd.execute();
            }
        });
    }

    @Deprecated
    public static EventBus getEventBus() {
        return new ErraiSimpleEventBusProvider().get();
    }

    @Deprecated
    public static MessageCenter getMessageCenter() {
        return MODULES.getMessageCenter();
    }

    @Deprecated
    public static BootstrapContext getBootstrapContext()
    {
        return INSTANCE.bootstrapContext;
    }

    @Deprecated
    public static HelpSystem getHelpSystem() {
        return MODULES.getHelpSystem();
    }


    @Deprecated
    public static native boolean visAPILoaded() /*-{

        return false; // prevent usage of charts within metrics

    }-*/;


    public static SubsystemRegistry getSubsystemRegistry() {
        return MODULES.getSubsystemRegistry();
    }

    public static RuntimeExtensionRegistry getRuntimeLHSItemExtensionRegistry() {
        return MODULES.getRuntimeLHSItemExtensionRegistry();
    }

    public static boolean protovisAvailable() {
        String userAgent = Window.Navigator.getUserAgent();
        return !(userAgent.contains("MSIE") || userAgent.contains("msie"));
    }


    @Override
    public void onReloadRequired(Map<String, ServerState> states) {

        ReloadState reloadState = Console.MODULES.getReloadState();
        reloadState.updateFrom(states);
        reloadState.propagateChanges();
    }

}
