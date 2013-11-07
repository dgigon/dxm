/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.services.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.registries.ServicesRegistry;

/**
 * Wrapper for the cache entry to use the classloader of the specified module.
 * 
 * @author Thomas Draier
 */
public class ClassLoaderAwareCacheEntry implements Serializable {
    private static final long serialVersionUID = -4281419239864698107L;

    private transient String moduleName;

    private Object value;

    public ClassLoaderAwareCacheEntry(Object value, String moduleName) {
        this.value = value;
        this.moduleName = moduleName;
    }

    public Object getValue() {
        return value;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        moduleName = (String) in.readObject();
        JahiaTemplatesPackage module = ServicesRegistry.getInstance().getJahiaTemplateManagerService()
                .getTemplatePackageByFileName(moduleName);
        ClassLoader moduleCL = module != null ? module.getClassLoader() : null;
        Thread currentThread = null;
        ClassLoader tccl = null;
        if (moduleCL != null) {
            currentThread = Thread.currentThread();
            tccl = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(moduleCL);
        }
        try {
            in.defaultReadObject();
        } finally {
            if (module != null) {
                currentThread.setContextClassLoader(tccl);
            }
        }
    }

    @SuppressWarnings("unused")
    private void readObjectNoData() throws ObjectStreamException {
        // do nothing
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(moduleName);
        out.defaultWriteObject();
    }

}
