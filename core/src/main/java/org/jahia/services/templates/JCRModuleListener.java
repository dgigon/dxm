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

package org.jahia.services.templates;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.ExternalEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;


public class JCRModuleListener  extends DefaultEventListener implements ExternalEventListener {

    private static Logger logger = LoggerFactory.getLogger(JCRModuleListener.class);
    
    private TemplatePackageRegistry packageRegistry;

    private Listener listener;

    public void setPackageRegistry(TemplatePackageRegistry packageRegistry) {
        this.packageRegistry = packageRegistry;
    }

    @Override
    public int getEventTypes() {
        return Event.NODE_ADDED;
    }

    @Override
    public String getPath() {
        return "/modules";
    }

    @Override
    public String[] getNodeTypes() {
        return new String[] {"jnt:moduleVersion"};
    }

    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            try {
                Event e = events.nextEvent();
                String path = e.getPath();
                String[] splitpath = path.split("/");
                JahiaTemplatesPackage p = packageRegistry.lookupByIdAndVersion(splitpath[2], new ModuleVersion(splitpath[3]));
                if (listener != null && p != null) {
                    listener.onModuleImported(p);
                }
            } catch (Exception e1) {
                logger.error("Error handling event", e1);
            }
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onModuleImported(JahiaTemplatesPackage pack);
    }
}