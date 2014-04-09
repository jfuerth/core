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

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.EntityFactory;
import org.jboss.as.console.client.widgets.forms.FormMetaData;
import org.jboss.as.console.client.widgets.forms.Mutator;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;

import com.google.gwt.core.client.GWT;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
@ApplicationScoped
public class ApplicationMetaDataBean implements ApplicationMetaData {

    private final ApplicationMetaData generated = GWT.create(ApplicationMetaData.class);

    @Override
    public List<PropertyBinding> getBindingsForType(Class<?> type) {
        return generated.getBindingsForType(type);
    }

    @Override
    public BeanMetaData getBeanMetaData(Class<?> type) {
        return generated.getBeanMetaData(type);
    }

    @Override
    public Mutator getMutator(Class<?> type) {
        return generated.getMutator(type);
    }

    @Override
    public <T> EntityFactory<T> getFactory(Class<T> type) {
        return generated.getFactory(type);
    }

    @Override
    public FormMetaData getFormMetaData(Class<?> type) {
        return generated.getFormMetaData(type);
    }

}
