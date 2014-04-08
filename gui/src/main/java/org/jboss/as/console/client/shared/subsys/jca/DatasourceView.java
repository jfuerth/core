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

package org.jboss.as.console.client.shared.subsys.jca;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;

/**
 * @author Heiko Braun
 * @date 3/24/11
 */
public class DatasourceView extends SuspendableViewImpl implements DataSourcePresenter.MyView {

    private DataSourcePresenter presenter;
    private DataSourceEditor dataSourceEditor;
    private XADataSourceEditor xaDataSourceEditor;

    @Override
    public Widget createWidget() {

        this.dataSourceEditor = new DataSourceEditor(presenter);
        this.xaDataSourceEditor = new XADataSourceEditor(presenter);

        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");


        tabLayoutpanel.add(dataSourceEditor.asWidget(), Console.CONSTANTS.subsys_jca_dataSources(), true);
        tabLayoutpanel.add(xaDataSourceEditor.asWidget(), Console.CONSTANTS.subsys_jca_dataSourcesXA(), true);

        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }

    @Override
    public void setXAProperties(String dataSourceName, List<PropertyRecord> result) {
        xaDataSourceEditor.setXaProperties(dataSourceName, result);
    }

    @Override
    public void setPresenter(DataSourcePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateDataSources(List<DataSource> datasources) {
        dataSourceEditor.updateDataSources(datasources);
    }

    @Override
    public void updateXADataSources(List<XADataSource> result) {
        xaDataSourceEditor.updateDataSources(result);
    }

    @Override
    public void enableDSDetails(boolean b) {
        dataSourceEditor.enableDetails(b);
    }

    @Override
    public void enableXADetails(boolean b) {
        xaDataSourceEditor.enableDetails(b);
    }

    @Override
    public void setPoolConfig(String name, PoolConfig poolConfig) {
        dataSourceEditor.setPoolConfig(name, poolConfig);
    }

    @Override
    public void setXAPoolConfig(String name, PoolConfig poolConfig) {
        xaDataSourceEditor.setPoolConfig(name, poolConfig);
    }

    public void setConnectionProperties(String reference, List<PropertyRecord> properties) {
        dataSourceEditor.setConnectionProperties(reference, properties);
    }

    @Override
    public void showVerifyConncectionResult(final String name, final VerifyConnectionOp.VerifyResult result) {
        new ConnectionWindow(name, result).show();
    }
}
