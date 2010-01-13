/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2010 Jahia Limited. All rights reserved.
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
 * in Jahia's FLOSS exception. You should have recieved a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license"
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.services.render.filter.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import org.apache.log4j.Logger;
import org.jahia.services.cache.CacheEntry;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.Script;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;

/**
 * Module content caching filter.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 8 janv. 2010
 */
public class CacheFilter extends AbstractFilter implements InitializingBean {
    private transient static Logger logger = Logger.getLogger(CacheFilter.class);
    private EhCacheProvider cacheProvider;
    public static final String CACHE_NAME = "CJHTMLCache";
    public static final String DEPS_CACHE_NAME = CACHE_NAME + "dependencies";
    private BlockingCache blockingCache;
    private Cache dependenciesCache;
    private int blockingTimeout = 5000;

    public BlockingCache getBlockingCache() {
        return blockingCache;
    }

    public Cache getDependenciesCache() {
        return dependenciesCache;
    }

    public void setBlockingTimeout(int blockingTimeout) {
        this.blockingTimeout = blockingTimeout;
    }

    @Override
    protected String execute(RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        if (!renderContext.isEditMode()) {
            Map<String, Map<String, Integer>> templatesCacheExpiration = renderContext.getTemplatesCacheExpiration();
            boolean debugEnabled = logger.isDebugEnabled();
            String key = generateKey(resource, renderContext);
            if(debugEnabled) {
                logger.debug("Cache filter for key "+key);
            }
            Element element = blockingCache.get(key);
            if (element != null) {
                if(debugEnabled) logger.debug("Getting content from cache for node : " + key);
                return (String) ((CacheEntry) element.getValue()).getObject();
            }
            else {
                if(debugEnabled) logger.debug("Generating content for node : " + key);
                String renderContent = chain.doFilter(renderContext, resource);
                final Script script = (Script) renderContext.getRequest().getAttribute("script");
                Long expiration = Long.parseLong(script.getTemplate().getProperties().getProperty("cache.expiration","-1"));
                List<JCRNodeWrapper> depNodeWrappers = resource.getDependencies();
                for (JCRNodeWrapper nodeWrapper : depNodeWrappers) {
                    Long lowestExpiration = 0L;
                    String path = nodeWrapper.getPath();
                    Map<String, Integer> cachesExpiration = templatesCacheExpiration.get(path);
                    if (cachesExpiration != null) {
                        for(long cacheExpiration : cachesExpiration.values()) {
                            lowestExpiration = Math.min(cacheExpiration,lowestExpiration);
                        }
                        expiration = lowestExpiration;
                    }
                    Element element1 = dependenciesCache.get(path);
                    Set<String> dependencies;
                    if (element1 != null) {
                        dependencies = (Set<String>) element1.getValue();
                    } else {
                        dependencies = new LinkedHashSet<String>();
                    }
                    dependencies.add(key);
                    dependenciesCache.put(new Element(path,dependencies));
                }
                CacheEntry<String> cacheEntry = new CacheEntry<String>(renderContent);
                Element cachedElement = new Element(key, cacheEntry);
                if (expiration >= 0) {
                    cachedElement.setTimeToLive(expiration.intValue()+1);
                    cachedElement.setTimeToIdle(1);
                    Map<String, Integer> cachesExpiration = templatesCacheExpiration.get(resource.getNode().getPath());
                    if (cachesExpiration == null) {
                        cachesExpiration = new HashMap<String,Integer>();
                    }
                    cachesExpiration.put(key,expiration.intValue());
                    templatesCacheExpiration.put(resource.getNode().getPath(),cachesExpiration);
                    final String hiddenKey = key.replaceAll("__template__(.*)__lang__",
                                                            "__template__hidden.load__lang__");
                    if(blockingCache.isKeyInCache(hiddenKey)) {
                        Element hiddenElement = blockingCache.get(hiddenKey);
                        hiddenElement.setTimeToIdle(1);
                        hiddenElement.setTimeToLive(expiration.intValue()+1);
                        blockingCache.put(hiddenElement);
                    }
                }
                blockingCache.put(cachedElement);
                
                if (debugEnabled) {
                    logger.debug("Caching content for node : " + key);
                    StringBuilder stringBuilder = new StringBuilder();
                    for (JCRNodeWrapper nodeWrapper : depNodeWrappers) {
                        stringBuilder.append(nodeWrapper.getPath()).append("\n");
                    }
                    logger.debug("Dependencies of " + key + " : \n" + stringBuilder.toString());
                }
                return renderContent;
            }
        }
        return chain.doFilter(renderContext, resource);
    }

    public String generateKey(Resource resource, RenderContext context) {
        return new StringBuilder().append(resource.getNode().getPath()).append("__template__").append(
                resource.getResolvedTemplate()).append("__lang__").append(resource.getLocale()).append(
                "__site__").append(context.getSite().getSiteKey()).toString();
    }

    public void setCacheProvider(EhCacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    public void afterPropertiesSet() throws Exception {
        CacheManager cacheManager = cacheProvider.getCacheManager();
        if (!cacheManager.cacheExists(CACHE_NAME)) {
            cacheManager.addCache(CACHE_NAME);
        }
        if (!cacheManager.cacheExists(DEPS_CACHE_NAME)) {
            cacheManager.addCache(DEPS_CACHE_NAME);
        }
        blockingCache = new BlockingCache(cacheManager.getCache(CACHE_NAME));
        blockingCache.setTimeoutMillis(blockingTimeout);
        dependenciesCache = cacheManager.getCache(DEPS_CACHE_NAME);
    }
}