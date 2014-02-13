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

import com.google.gwt.core.client.GWT;
import org.jboss.as.console.client.ProductConfig;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
@POC
public class ProductConfigPOC implements ProductConfig {

    private static final ProductConfig generated = GWT.create(ProductConfig.class);

    @Override
    public Profile getProfile() {
        return generated.getProfile();
    }

    @Override
    public String getCoreVersion() {
        return generated.getCoreVersion();
    }

    @Override
    public String getConsoleVersion() {
        return generated.getConsoleVersion();
    }

    @Override
    public String getProductName() {
        return generated.getProductName();
    }

    @Override
    public String getProductVersion() {
        return generated.getProductVersion();
    }

    @Override
    public String getDevHost() {
        return generated.getDevHost();
    }

}
