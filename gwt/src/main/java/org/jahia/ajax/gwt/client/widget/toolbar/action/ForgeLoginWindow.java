/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.ajax.gwt.client.widget.toolbar.action;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.*;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.user.client.Element;
import org.jahia.ajax.gwt.client.core.JahiaGWTParameters;
import org.jahia.ajax.gwt.client.data.GWTModuleReleaseInfo;
import org.jahia.ajax.gwt.client.messages.Messages;

import java.util.ArrayList;
import java.util.List;

public class ForgeLoginWindow extends Window {
    public static String username = "";
    public static String password = "";

    public interface Callback {
        void handle(String username, String password);
    }

    private Callback callback;

    private GWTModuleReleaseInfo releaseInfo;

    public ForgeLoginWindow() {
        super();
    }

    @Override
    protected void onRender(Element element, int index) {
        super.onRender(element, index);

        setLayout(new FitLayout());
        setHeadingHtml(Messages.get("label.login", "Login"));
        setModal(true);
        setWidth(500);
        setHeight(150);

        final FormPanel formPanel = new FormPanel();
        formPanel.setHeaderVisible(false);
        formPanel.setLabelWidth(150);
        formPanel.setButtonAlign(Style.HorizontalAlignment.CENTER);

        final TextField<String>tfUsername = new TextField<String>();
        tfUsername.setFieldLabel(Messages.get("label.username", "Username"));
        tfUsername.setValue(username);
        formPanel.add(tfUsername);

        final TextField<String> tfPassword = new TextField<String>();
        tfPassword.setFieldLabel(Messages.get("label.password", "Password"));
        tfPassword.setValue(password);
        tfPassword.setPassword(true);
        formPanel.add(tfPassword);

        Button b = new Button(Messages.get("label.login", "Login"), new SelectionListener<ButtonEvent>() {
            public void componentSelected(ButtonEvent event) {
                ForgeLoginWindow.username = tfUsername.getValue();
                ForgeLoginWindow.password = tfPassword.getValue();
                callback.handle(tfUsername.getValue(), tfPassword.getValue());
            }
        });
        formPanel.addButton(b);

        final Window w = this;
        b = new Button(Messages.get("label.cancel", "Cancel"), new SelectionListener<ButtonEvent>() {
            public void componentSelected(ButtonEvent event) {
                w.hide();
            }
        });
        formPanel.addButton(b);

        add(formPanel);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }


}