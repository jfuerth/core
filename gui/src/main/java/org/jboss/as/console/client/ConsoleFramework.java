package org.jboss.as.console.client;

import java.lang.annotation.Annotation;

import org.jboss.as.console.client.poc.POC;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.ballroom.client.rbac.SecurityService;
import org.jboss.ballroom.client.spi.Framework;
import org.jboss.errai.ioc.client.container.IOC;

import com.google.gwt.core.client.GWT;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.proxy.PlaceManager;

/**
 * @author Heiko Braun
 * @date 7/12/11
 */
public class ConsoleFramework implements Framework {

    private final static BeanFactory factory = GWT.create(BeanFactory.class);

    @Override
    public EventBus getEventBus() {
        return Console.getEventBus();
    }

    @Override
    public PlaceManager getPlaceManager() {
        return IOC.getBeanManager().lookupBean(PlaceManager.class, new POC() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return POC.class;
            }
        }).getInstance();
    }

    @Override
    public AutoBeanFactory getBeanFactory() {
        return factory;
    }

    @Override
    public SecurityService getSecurityService() {
        return IOC.getBeanManager().lookupBean(SecurityFramework.class).getInstance();
    }
}
