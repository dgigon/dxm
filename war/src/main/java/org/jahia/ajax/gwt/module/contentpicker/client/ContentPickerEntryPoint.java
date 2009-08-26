/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Solutions Group SA. All rights reserved.
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
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.ajax.gwt.module.contentpicker.client;

import org.jahia.ajax.gwt.client.util.JahiaGWT;
import org.jahia.ajax.gwt.client.widget.content.ContentPicker;
import org.jahia.ajax.gwt.client.widget.content.ContentPickerViewport;
import org.jahia.ajax.gwt.client.widget.content.util.ContentHelper;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.DOM;
import com.google.gwt.core.client.EntryPoint;

import java.util.List;

/**
 * File picker to embed in container edition engine or anywhere else you want,
 * provided there is correct initialization.
 *
 * @author rfelden
 *         Date: 27 ao�t 2008
 *         Time: 17:59:59
 */
public class ContentPickerEntryPoint implements EntryPoint {
    public void onModuleLoad() {
        JahiaGWT.init();
        RootPanel panel = RootPanel.get("contentmanager");
        if (panel != null) {
            String callback = DOM.getElementAttribute(panel.getElement(), "callback") ;
            final List<GWTJahiaNode> selectedContentNodes = ContentHelper.getSelectedContentNodesFromHTML();

            if (callback == null || callback.length() == 0) {
                panel.add(new ContentPicker(
                        DOM.getElementAttribute(panel.getElement(), "rootPath"),
                        selectedContentNodes,
                        DOM.getElementAttribute(panel.getElement(), "nodeTypes"),
                        DOM.getElementAttribute(panel.getElement(), "filters"),
                        DOM.getElementAttribute(panel.getElement(), "mimeTypes"),
                        DOM.getElementAttribute(panel.getElement(), "config"),
                        Boolean.parseBoolean(DOM.getElementAttribute(panel.getElement(), "multiple")),
                        Boolean.parseBoolean(DOM.getElementAttribute(panel.getElement(), "allowThumbs")),
                        callback));
            } else {
                panel.add(new ContentPickerViewport(
                        DOM.getElementAttribute(panel.getElement(), "rootPath"),
                        selectedContentNodes,
                        DOM.getElementAttribute(panel.getElement(), "nodeTypes"),
                        DOM.getElementAttribute(panel.getElement(), "filters"),
                        DOM.getElementAttribute(panel.getElement(), "mimeTypes"),
                        DOM.getElementAttribute(panel.getElement(), "config"),
                        Boolean.parseBoolean(DOM.getElementAttribute(panel.getElement(), "multiple")),
                        Boolean.parseBoolean(DOM.getElementAttribute(panel.getElement(), "allowThumbs")),
                        callback));
            }
        }
    }

}
