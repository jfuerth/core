/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.shared.patching.wizard.rollback;

import java.util.List;

import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.wizard.CommonPatchContext;
import org.jboss.dmr.client.ModelNode;

/**
 * Simple 'struct' for data exchange between rollback wizard steps.
 *
 * @author Harald Pehl
 */
public class RollbackContext extends CommonPatchContext {

    // initial data
    final PatchInfo patchInfo;
    // process slip
    boolean overrideAll;
    boolean resetConfiguration;
    boolean rollbackError;
    String rollbackErrorDetails;

    public RollbackContext(final boolean standalone, final String host, final List<String> runningServers,
            final ModelNode patchAddress, final PatchInfo patchInfo) {

        super(standalone, host, runningServers, patchAddress);
        this.patchInfo = patchInfo;
    }
}
