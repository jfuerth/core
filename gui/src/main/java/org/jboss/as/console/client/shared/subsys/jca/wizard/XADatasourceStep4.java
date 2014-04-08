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

package org.jboss.as.console.client.shared.subsys.jca.wizard;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.widgets.forms.items.NonRequiredTextBoxItem;
import org.jboss.ballroom.client.widgets.forms.ButtonItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 4/18/11
 */
public class XADatasourceStep4 {


    NewXADatasourceWizard wizard;
    Form<XADataSource> form ;

    public XADatasourceStep4(NewXADatasourceWizard wizard) {
        this.wizard = wizard;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        layout.add(new HTML("<h3>"+ Console.CONSTANTS.subsys_jca_xadataSource_step4()+"</h3>"));

        form = new Form<XADataSource>(XADataSource.class);

        TextBoxItem user = new NonRequiredTextBoxItem("username", "Username");
        PasswordBoxItem pass = new PasswordBoxItem("password", "Password") {
            {
                setRequired(false);
            }
        };
        TextBoxItem domain = new NonRequiredTextBoxItem("securityDomain", "Security Domain");

        ButtonItem testBtn = new ButtonItem("testConnection", "", Console.CONSTANTS.subsys_jca_dataSource_verify());
        testBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                FormValidation validation = form.validate();
                if (!validation.hasErrors()) {
                    wizard.onVerifyConnection(form.getUpdatedEntity(), true, false);
                }
            }
        });

        form.setFields(user, pass, domain, testBtn);

        final FormHelpPanel helpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = Baseadress.get();
                        address.add("subsystem", "datasources");
                        address.add("xa-data-source", "*");
                        return address;
                    }
                }, form
        );

        layout.add(helpPanel.asWidget());

        layout.add(form.asWidget());

        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                FormValidation validation = form.validate();
                if(!validation.hasErrors())
                {
                    wizard.onFinish(form.getUpdatedEntity());
                }
            }
        };

        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                wizard.getPresenter().closeDialogue();
            }
        };

        DialogueOptions options = new DialogueOptions(
                "Done",submitHandler,
                Console.CONSTANTS.common_label_cancel(),cancelHandler
        );

        return new WindowContentBuilder(layout,options).build();
    }

    void edit(XADataSource entity)
    {
        form.edit(entity);
    }
}
