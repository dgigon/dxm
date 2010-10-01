/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2010 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.ajax.gwt.client.widget.edit;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.Style;
import org.jahia.ajax.gwt.client.data.toolbar.GWTEditConfiguration;
import org.jahia.ajax.gwt.client.widget.edit.mainarea.MainModule;
import org.jahia.ajax.gwt.client.widget.edit.sidepanel.SidePanel;
import org.jahia.ajax.gwt.client.widget.toolbar.ActionToolbarLayoutContainer;

/**
 * Edit mode manager widget.
 * User: toto
 * Date: Aug 18, 2009
 * Time: 5:55:47 PM
 */
public class EditManager extends ContentPanel {

    private MainModule mainModule;
    private SidePanel sidePanel;
    private ActionToolbarLayoutContainer toolbar;
    private EditLinker editLinker;
    private BorderLayout borderLayout ;

    public EditManager(String html, String path, String template, String nodeTypes, String locale, GWTEditConfiguration config) {
        long start = System.currentTimeMillis();
        borderLayout =  new BorderLayout();
        setLayout(borderLayout);
        setHeaderVisible(false);
        BorderLayoutData data = new BorderLayoutData(Style.LayoutRegion.WEST, 300);
        data.setCollapsible(true);
        data.setSplit(true);
        data.setFloatable(true);
        sidePanel = new SidePanel(config);
        sidePanel.setStyleAttribute("z-index", "999");
        sidePanel.addStyleName("gwt-only-panel");
        add(sidePanel, data);

        toolbar =  new ActionToolbarLayoutContainer(config.getTopToolbar());
        toolbar.setStyleAttribute("z-index", "999");
        toolbar.setStyleAttribute("position", "relative");
        toolbar.addStyleName("gwt-only-panel");
        setTopComponent(toolbar);

        setScrollMode(Style.Scroll.NONE);
        mainModule = new MainModule(html, path, template, nodeTypes, config);
        mainModule.getHeader().addStyleName("gwt-only-panel");        
        add(mainModule, new BorderLayoutData(Style.LayoutRegion.CENTER));

        editLinker = new EditLinker(mainModule, sidePanel, toolbar, config);
        editLinker.setLocale(locale);

        Log.info("Edit manager inited : " + (System.currentTimeMillis() - start));




    }


    public MainModule getMainModule() {
        return mainModule;
    }

    public SidePanel getSidePanel() {
        return sidePanel;
    }

    public ActionToolbarLayoutContainer getToolbar() {
        return toolbar;
    }

    public EditLinker getEditLinker() {
        return editLinker;
    }

    


}
