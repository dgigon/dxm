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

package org.jahia.services.notification;

import static org.apache.commons.httpclient.HttpStatus.SC_OK;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.apache.taglibs.standard.tag.common.core.ImportSupport;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.StringResponseWrapper;
import org.springframework.web.context.ServletContextAware;

/**
 * Utility class for HTTP communication.<br>
 * Parts of it are based on the code of the JSTL's &lt;c:import&gt; tag, for
 * reading the content of the provided URL.
 * 
 * @author Sergiy Shyrkov
 */
public class HttpClientService implements ServletContextAware {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(HttpClientService.class);

    /**
     * Returns <tt>true</tt> if our current URL is absolute, <tt>false</tt>
     * otherwise.
     * 
     * @see ImportSupport#isAbsoluteUrl(String)
     */
    public static boolean isAbsoluteUrl(String url) {
        return ImportSupport.isAbsoluteUrl(url);
    }

    private HttpClient httpClient;

    private ServletContext servletContext;

    /**
     * Connects to the specified absolute URL and reads its content as a string.
     * 
     * @param url the absolute URL to connect to
     * @return the string representation of the URL connection response
     * @throws {@link IllegalArgumentException} in case of a malformed URL
     */
    public String getAbsoluteResourceAsString(String url) throws IllegalArgumentException {
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Provided URL is null");
        }
        if (!isAbsoluteUrl(url)) {
            throw new IllegalArgumentException("Cannot handle non-absolute URL: " + url);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Asked to get content from the URL: " + url);
        }

        String content = null;

        GetMethod httpMethod = new GetMethod(url);

        try {
            httpClient.executeMethod(httpMethod);
            StatusLine statusLine = httpMethod.getStatusLine();

            if (statusLine != null && statusLine.getStatusCode() == SC_OK) {
                content = httpMethod.getResponseBodyAsString();
            } else {
                logger.warn("Connection to URL: " + url + " failed with status " + statusLine);
            }

        } catch (HttpException e) {
            logger.error("Unable to get the content of the URL: " + url + ". Cause: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Unable to get the content of the URL: " + url + ". Cause: " + e.getMessage(), e);
        } finally {
            httpMethod.releaseConnection();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved " + (content != null ? content.length() : 0) + " characters as a response");
            if (logger.isTraceEnabled()) {
                logger.trace("Content:\n" + content);
            }
        }

        return content;
    }

    /**
     * Retrieves the content of the resource, specified by the provided URL. The
     * resource should be available in the current servlet context.
     * 
     * @param url the URL of the resource to be retrieved
     * @return the string content of the resource
     * @throws {@link IllegalArgumentException} in case of a malformed URL
     * @see ServletContext#getResourceAsStream(String)
     */
    public String getContextResourceAsString(String url) throws IllegalArgumentException {
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Provided URL is null");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Asked to get content from the context resource: " + url);
        }

        String content = null;
        InputStream is = servletContext.getResourceAsStream(url);
        if (is != null) {
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(is, writer, SettingsBean.getInstance().getCharacterEncoding());
                content = writer.toString();
            } catch (IOException e) {
                logger.warn("Error reading content of the resource " + url + ". Cause: " + e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            logger.warn("Unable to find context resource at path " + url);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved " + (content != null ? content.length() : 0) + " characters as a response");
            if (logger.isTraceEnabled()) {
                logger.trace("Content:\n" + content);
            }
        }

        return content;
    }

    /**
     * Retrieves the content of the specified resource as a string. If the
     * absolute URL is provided, an HTTP connection is used to get the response.
     * Otherwise the resource is considered to be current context resource and
     * is retrieved using {@link ServletContext#getResource(String)} method.
     * 
     * @param url the resource URL
     * @return the string representation of the resource content
     * @throws {@link IllegalArgumentException} in case of a malformed URL
     */
    public String getResourceAsString(String url) throws IllegalArgumentException {
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Provided URL is null");
        }
        return isAbsoluteUrl(url) ? getAbsoluteResourceAsString(url) : getContextResourceAsString(url);
    }

    /**
     * Retrieves resource content, specified by the provided URL.
     * 
     * @param url the URL of the resource to be retrieved
     * @return the string content of the resource
     * @throws {@link IllegalArgumentException} in case of a malformed URL
     */
    public String getResourceAsString(String url, HttpServletRequest request, HttpServletResponse response)
            throws IllegalArgumentException {
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Provided URL is null");
        }
        if (isAbsoluteUrl(url)) {
            return getAbsoluteResourceAsString(url);
        }
        if (!url.startsWith("/")) {
            throw new IllegalArgumentException("Provided relative URL does not start with a '/'");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Asked to get content from the URL: " + url);
        }

        String content = null;

        RequestDispatcher rd = request.getRequestDispatcher(url);
        if (rd != null) {
            StringResponseWrapper wrapper = new StringResponseWrapper(response);
            try {
                rd.include(request, wrapper);
                if (wrapper.getStatus() < 200 || wrapper.getStatus() > 299) {
                    logger.warn("Unable to get the content of the resource " + url + ". Got response status code: "
                            + wrapper.getStatus());
                } else {
                    content = wrapper.getString();
                }
            } catch (Exception e) {
                logger.warn("Unable to get the content of the resource " + url + ". Cause: " + e.getMessage(), e);
            }
        } else {
            logger.warn("Unable to get a RequestDispatcher for the path " + url);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved " + (content != null ? content.length() : 0) + " characters as a response");
            if (logger.isTraceEnabled()) {
                logger.trace("Content:\n" + content);
            }
        }

        return content;
    }

    /**
     * Injects an instance of the {@link HttpClient}.
     * 
     * @param httpClient an instance of the {@link HttpClient}
     */
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void shuddown() {
        logger.info("Shutting down HttpClient...");
        try {
            MultiThreadedHttpConnectionManager.shutdownAll();
        } catch (Exception e) {
            logger.warn("Error shutting down HttpClient. Cause: " + e.getMessage(), e);
        }
        logger.info("...done");
    }

    /**
     * @return the httpClient
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }
}
