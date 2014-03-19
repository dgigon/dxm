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

package org.jahia.services.content.nodetypes.initializers;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.security.JahiaPrivilegeRegistry;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.utils.i18n.Messages;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;
import java.util.*;

public class PermissionsChoiceListInitializer implements ChoiceListInitializer {
    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(PermissionsChoiceListInitializer.class);

    @Override
    public List<ChoiceListValue> getChoiceListValues(ExtendedPropertyDefinition epd, String param, List<ChoiceListValue> values, Locale locale, Map<String, Object> context) {
        List<ChoiceListValue> result = new ArrayList<ChoiceListValue>();
        Privilege[] p = JahiaPrivilegeRegistry.getRegisteredPrivileges();
        try {
            for (Privilege privilege : p) {
                final String jcrName = JCRContentUtils.getJCRName(privilege.getName(), JCRSessionFactory.getInstance().getNamespaceRegistry());
                String localName = jcrName;
                if (localName.contains(":")) {
                    localName = StringUtils.substringAfter(localName, ":");
                }
                String title = StringUtils.capitalize(localName.replaceAll("([A-Z])", " $0").replaceAll("[_-]", " ").toLowerCase());
                final String rbName = localName.replaceAll("-", "_");
                result.add(new ChoiceListValue(Messages.getInternal("label.permission." + rbName, locale, title), jcrName));
            }
        } catch (RepositoryException e) {
            logger.error("Repository exception", e);
        }
        Collections.sort(result);
        return result;
    }
}