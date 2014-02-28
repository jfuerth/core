package org.jboss.as.console.client.core;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.as.console.client.poc.POC;

import com.gwtplatform.mvp.client.proxy.Gatekeeper;

/**
 * @author Heiko Braun
 * @date 1/26/12
 */
@Singleton
public class DomainGateKeeper implements Gatekeeper {


    private final BootstrapContext bootstrapContext;

    @Inject
    public DomainGateKeeper(final @POC BootstrapContext bootstrapContext) {
        this.bootstrapContext = bootstrapContext;
    }

    @Override
    public boolean canReveal() {
        return !bootstrapContext.isStandalone();
    }
}