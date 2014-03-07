package org.jboss.as.console.client.rbac;

import org.jboss.as.console.client.core.gin.ErraiPlaceManagerProvider;

/**
 * Resolves security context from place requests
 *
 * @author Heiko Braun
 * @date 8/12/13
 */
public class PlaceSecurityResolver implements ContextKeyResolver {
    @Override
    public String resolveKey() {
        return new ErraiPlaceManagerProvider().get().getCurrentPlaceRequest().getNameToken();
    }
}
