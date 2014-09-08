/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.services.tags;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.query.QOMBuilder;
import org.jahia.services.query.QueryResultWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kevan on 03/09/14.
 */
public class TagsSuggesterImpl implements TagsSuggester{
    private static Logger logger = LoggerFactory.getLogger(TagsSuggesterImpl.class);
    private boolean faceted = false;

    @Override
    public Map<String, Long> suggest(String input, String startPath, Long mincount, Long limit, Long offset,
                                     boolean sortByCount, JCRSessionWrapper sessionWrapper) throws RepositoryException {

        if(faceted){
            return facetedSuggestion(input, startPath, mincount, limit, offset, sortByCount, sessionWrapper);
        } else {
            return simpleSuggestion(input, startPath, limit, sessionWrapper);
        }
    }

    public Map<String, Long> facetedSuggestion (String prefix, String startPath, Long mincount, Long limit, Long offset,
                                               boolean sortByCount, JCRSessionWrapper sessionWrapper) throws RepositoryException {
        LinkedHashMap<String, Long> tagsMap = new LinkedHashMap<String, Long>();

        QueryManager queryManager = sessionWrapper.getWorkspace().getQueryManager();
        if (queryManager == null) {
            logger.error("Unable to obtain QueryManager instance");
            return tagsMap;
        }

        if(StringUtils.isEmpty(startPath)){
            startPath = "/sites";
        }

        StringBuilder facet = new StringBuilder();
        facet.append("rep:facet(nodetype=jmix:tagged&key=j:tagList")
                .append(mincount != null ? "&facet.mincount=" + mincount.toString() : "")
                .append(limit != null ? "&facet.limit=" + limit.toString() : "")
                .append(offset != null ? "&facet.offset=" + offset.toString() : "")
                .append("&facet.sort=").append(String.valueOf(sortByCount))
                .append(StringUtils.isNotEmpty(prefix) ? "&facet.prefix=" + prefix : "")
                .append(")");

        QueryObjectModelFactory factory = queryManager.getQOMFactory();
        QOMBuilder qomBuilder = new QOMBuilder(factory, sessionWrapper.getValueFactory());

        qomBuilder.setSource(factory.selector("jmix:tagged", "tagged"));
        qomBuilder.andConstraint(factory.descendantNode("tagged", startPath));
        qomBuilder.getColumns().add(factory.column("tagged", "j:tagList", facet.toString()));

        QueryObjectModel qom = qomBuilder.createQOM();
        QueryResultWrapper res = (QueryResultWrapper) qom.execute();

        if(res.getFacetField("j:tagList").getValues() != null){
            for(FacetField.Count count : res.getFacetField("j:tagList").getValues()){
                tagsMap.put(count.getName(), count.getCount());
            }
        }

        return tagsMap;
    }

    public Map<String, Long> simpleSuggestion (String term, String startPath, Long limit, JCRSessionWrapper sessionWrapper) throws RepositoryException {
        LinkedHashMap<String, Long> tagsMap = new LinkedHashMap<String, Long>();

        QueryManager queryManager = sessionWrapper.getWorkspace().getQueryManager();
        if (queryManager == null) {
            logger.error("Unable to obtain QueryManager instance");
            return tagsMap;
        }

        if(StringUtils.isEmpty(startPath)){
            startPath = "/sites";
        }

        Query query = queryManager.createQuery("select t.[j:tagList] from [jmix:tagged] as t where " +
                "isdescendantnode(t, [" + startPath + "]) and t.[j:tagList] like '%" + term + "%'", Query.JCR_SQL2);
        QueryResult queryResult = query.execute();

        NodeIterator nodeIterator = queryResult.getNodes();
        boolean limitReached = false;
        while (!limitReached && nodeIterator.hasNext()) {
            JCRNodeWrapper nodeWrapper = (JCRNodeWrapper) nodeIterator.next();
            JCRValueWrapper[] tags = nodeWrapper.getProperty("j:tagList").getValues();
            for (JCRValueWrapper tag : tags) {
                String tagValue = tag.getString();
                if (tagValue.contains(term)) {
                    if (tagsMap.keySet().size() < limit) {
                        tagsMap.put(tagValue, 0l);
                    }else {
                        // limit reached
                        limitReached = true;
                        break;
                    }
                }
            }
        }

        return tagsMap;
    }

    public void setFaceted(boolean faceted) {
        this.faceted = faceted;
    }
}