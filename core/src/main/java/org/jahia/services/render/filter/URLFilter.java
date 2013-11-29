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

package org.jahia.services.render.filter;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.HtmlTagAttributeTraverser.HtmlTagAttributeVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Traverses the content and searches for URLs in the configured elements.
 * Executes the list of configured visitors to modify the URL value.
 */
public class URLFilter extends AbstractFilter {

    /**
     * Initializes an instance of this class.
     *
     * @param urlTraverser the URL utility class to visit HTML tag attributes
     */
    public URLFilter(HtmlTagAttributeTraverser urlTraverser) {
        super();
        this.urlTraverser = urlTraverser;
    }

    private HtmlTagAttributeTraverser urlTraverser;

    private HtmlTagAttributeVisitor[] handlers;

    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain)
            throws Exception {
        if (handlers != null && handlers.length > 0) {
<<<<<<< .working

            final String thisuuid = StringUtils.leftPad(Integer.toHexString(resource.hashCode()),8,"0");

            Map<String, String> m = new HashMap<String, String>();

            StringBuilder sb = new StringBuilder(previousOut);
            int i;
            final String startTag = "<!-- jahia:temp value=\"URLParserStart";
            while ((i = sb.indexOf(startTag)) > -1) {
                String uuid = sb.substring(i+ startTag.length(), i+ startTag.length() +8);
                final String endTag = "<!-- jahia:temp value=\"URLParserEnd" + uuid + "\" -->";
                int j = sb.indexOf(endTag);
                m.put(uuid, sb.substring(i, j + endTag.length()));
                sb.replace(i, j + endTag.length(), "<jahia:URLParserParsedReplaced id=\"" + uuid + "\"/>");
            }

            StringBuilder replaced = new StringBuilder("<!-- jahia:temp value=\"URLParserStart" + thisuuid + "\" -->" + urlTraverser.traverse(sb.toString(), renderContext, resource, handlers) + "<!-- jahia:temp value=\"URLParserEnd" + thisuuid + "\" -->");

            final String str2 = "<jahia:URLParserParsedReplaced id=\"";
            while ((i = replaced.indexOf(str2)) > -1) {
                String uuid = replaced.substring(i+str2.length(), i+str2.length() + 8);
                replaced.replace(i, i+str2.length()+ 8 + 3, m.get(uuid));
            }
            return replaced.toString();
=======

            final String thisuuid = StringUtils.leftPad(Integer.toHexString(resource.hashCode()), 8, "0");

            Map<String, String> m = new HashMap<String, String>();

            StringBuilder sb = new StringBuilder(previousOut);
            int i;
            final String startTag = "<!-- jahia:temp value=\"URLParserStart";
            while ((i = sb.indexOf(startTag)) > -1) {
                String uuid = sb.substring(i+ startTag.length(), i+ startTag.length() +8);
                final String endTag = "<!-- jahia:temp value=\"URLParserEnd" + uuid + "\" -->";
                int j = sb.indexOf(endTag);
                m.put(uuid, sb.substring(i, j + endTag.length()));
                sb.replace(i, j + endTag.length(), "<jahia:URLParserParsedReplaced id=\"" + uuid + "\"/>");
            }

            StringBuilder replaced = new StringBuilder("<!-- jahia:temp value=\"URLParserStart" + thisuuid + "\" -->" + urlTraverser.traverse(sb.toString(), renderContext, resource, handlers) + "<!-- jahia:temp value=\"URLParserEnd" + thisuuid + "\" -->");

            final String str2 = "<jahia:URLParserParsedReplaced id=\"";
            while ((i = replaced.indexOf(str2)) > -1) {
                String uuid = replaced.substring(i+str2.length(), i+str2.length() + 8);
                replaced.replace(i, i+str2.length()+ 8 + 3, m.get(uuid));
            }
            return replaced.toString();
>>>>>>> .merge-right.r47998
        }

        return previousOut;
    }

    /**
     * @param visitors the visitors to set
     */
    public void setHandlers(HtmlTagAttributeVisitor... visitors) {
        this.handlers = visitors;
    }

}
