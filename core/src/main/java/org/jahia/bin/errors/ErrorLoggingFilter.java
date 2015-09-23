/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.bin.errors;

import org.apache.commons.collections.iterators.EnumerationIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaException;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.settings.SettingsBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * Error logging filter that is called before an error page, configured in the
 * Web application deployment descriptor.
 *
 * @author Sergiy Shyrkov
 */
public class ErrorLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingFilter.class);

    private static Throwable previousException = null;
    private static int previousExceptionOccurrences = 0;

    @Override
    public void destroy() {
        ErrorFileDumper.shutdown();
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        Boolean alreadyForwarded = (Boolean) request.getAttribute("org.jahia.exception.forwarded");
        if (alreadyForwarded == null || !alreadyForwarded.booleanValue()) {
            handle((HttpServletRequest) request, (HttpServletResponse) response);
        }
        filterChain.doFilter(request, response);
    }


    protected static void dumpToFile(HttpServletRequest request) {
        try {
            Throwable t = getException(request);
            int code = (Integer) request.getAttribute("javax.servlet.error.status_code");
            code = code != 0 ? code : SC_INTERNAL_SERVER_ERROR;
            if (code < 500) {
                logger.debug("Status code below 500, will not dump error to file");
                return;
            }
            if (!ErrorFileDumper.isShutdown()) {
                ErrorFileDumper.dumpToFile(t, request);
            }
        } catch (Exception throwable) {
            logger.warn("Error creating error file", throwable);
        }
    }

    protected static void emailAlert(HttpServletRequest request, HttpServletResponse response) {

        Throwable exception = getException(request);
        try {

            Throwable previousExceptionToMail;
            int previousExceptionOccurrencesToMail;

            synchronized (ErrorLoggingFilter.class) {
                if (previousException != null && exception != null && exception.toString().equals(previousException.toString())) {
                    previousExceptionOccurrences++;
                    if (previousExceptionOccurrences < SettingsBean.getInstance().getMail_maxRegroupingOfPreviousException()) {
                        return;
                    }
                }
                previousExceptionToMail = previousException;
                previousExceptionOccurrencesToMail = previousExceptionOccurrences;
                previousException = exception;
                previousExceptionOccurrences = 1;
            }

            StringWriter msgBodyWriter = ErrorFileDumper.generateErrorReport(new ErrorFileDumper.HttpRequestData(request), exception, previousExceptionOccurrencesToMail, previousExceptionToMail);
            ServicesRegistry.getInstance().getMailService().sendMessage(null, null, null, null, "Server Error: " + (exception != null ? exception.getMessage() : ""), msgBodyWriter.toString());

            logger.debug("Mail was sent successfully.");
        } catch (Exception ex) {
            logger.warn("Error sending an e-mail alert: " + ex.getMessage(), ex);
        }
    }


    protected static Throwable getException(HttpServletRequest request) {
        Throwable ex = (Throwable) request.getAttribute("javax.servlet.error.exception");
        ex = ex != null ? ex : (Throwable) request.getAttribute("org.jahia.exception");
        return ex;
    }

    protected static String getLogMessage(HttpServletRequest request) {

        Throwable ex = getException(request);
        String message = (String) request.getAttribute("javax.servlet.error.message");
        Integer code = (Integer) request.getAttribute("javax.servlet.error.status_code");

        switch (code.intValue()) {
            case SC_NOT_FOUND:
                message = "Requested resource is not available: "
                        + request.getAttribute("javax.servlet.error.request_uri");
                break;

            case SC_UNAUTHORIZED:
                message = "Authorization required for resource: "
                        + request.getAttribute("javax.servlet.error.request_uri");
                break;

            case SC_FORBIDDEN:
                message = "Access denied for resource: "
                        + request.getAttribute("javax.servlet.error.request_uri");
                break;

            default:
                if (message != null) {
                    if (ex != null && StringUtils.isNotEmpty(ex.getMessage())
                            && !message.equals(ex.getMessage())) {
                        message = message + ". Error message: " + ex.getMessage();
                    }
                } else {
                    if (ex != null && StringUtils.isNotEmpty(ex.getMessage())) {
                        message = ex.getMessage();
                    } else {
                        message = "Unexpected exception occurred";
                    }
                }
                break;
        }

        if (logger.isInfoEnabled()) {
            String requestInfo = (String) request
                    .getAttribute("org.jahia.exception.requestInfo");
            if (requestInfo != null) {
                message = message
                        + "\n"
                        + request
                        .getAttribute("org.jahia.exception.requestInfo");
            }
        }

        return message;
    }

    protected static void handle(HttpServletRequest request, HttpServletResponse response) {

        // add request information
        request.setAttribute("org.jahia.exception.requestInfo", getRequestInfo(request));

        logDebugInfo(request, response);

        if (HttpServletResponse.SC_SERVICE_UNAVAILABLE == (Integer) request
                .getAttribute("javax.servlet.error.status_code")
                && (StringUtils.equals(ErrorServlet.MAINTENANCE_MODE,
                        (String) request.getAttribute("javax.servlet.error.message")) || StringUtils
                        .equals(ErrorServlet.LICENSE_TERMS_VIOLATION_MODE,
                                (String) request.getAttribute("javax.servlet.error.message")))) {
            return;
        }

        logException(request, response);

        if (SettingsBean.getInstance().isDumpErrorsToFiles()) {
            dumpToFile(request);
        }

        if (isMailServiceEnabled() && isEmailAlertRequired(request, response)) {
            emailAlert(request, response);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig cfg) throws ServletException {
        if (ErrorFileDumper.isShutdown()) {
            ErrorFileDumper.start();
        }
    }

    protected static boolean isEmailAlertRequired(HttpServletRequest request, HttpServletResponse response) {

        Throwable error = getException(request);

        return error != null
                && (error instanceof JahiaException)
                && ServicesRegistry.getInstance().getMailService().getSettings().getNotificationSeverity() != 0
                && ServicesRegistry.getInstance().getMailService().getSettings().getNotificationSeverity() <= ((JahiaException) error).getSeverity();
    }

    private static boolean isMailServiceEnabled() {
        return ServicesRegistry.getInstance().getMailService().isEnabled();
    }

    protected static void logDebugInfo(HttpServletRequest request, HttpServletResponse response) {

        if (logger.isDebugEnabled()) {
            logger.debug("Handling exception for request ["
                    + request.getAttribute("javax.servlet.error.request_uri")
                    + "]:"
                    + "\n"
                    + "Status code: "
                    + request.getAttribute("javax.servlet.error.status_code")
                    + "\n"
                    + "Error message: "
                    + request.getAttribute("javax.servlet.error.message")
                    + "\n"
                    + "Exception type: "
                    + request
                    .getAttribute("javax.servlet.error.exception_type")
                    + "\n" + "Exception: "
                    + request.getAttribute("javax.servlet.error.exception")
                    + "\n" + "Servlet name: "
                    + request.getAttribute("javax.servlet.error.servlet_name"));
        }
    }

    protected static void logException(HttpServletRequest request, HttpServletResponse response) {

        Throwable ex = getException(request);

        int code = (Integer) request
                .getAttribute("javax.servlet.error.status_code");
        code = code != 0 ? code : SC_INTERNAL_SERVER_ERROR;

        String message = getLogMessage(request);

        if (code >= 500) {
            if (ex != null) {
                logger.error(message, ex);
            } else {
                logger.error(message);
            }
        } else {
            if (ex != null && logger.isDebugEnabled()) {
                logger.debug(message, ex);
            } else {
                if (SC_UNAUTHORIZED == code) {
                    logger.info(message);
                } else {
                    logger.warn("[Error code: " + code + "]"+ (message != null && message.length() > 0 ? ": " + message : ""));
                }
            }
        }
    }

    /**
     * Returns the request information for logging purposes.
     *
     * @param request the http request object
     * @return the request information for logging purposes
     */
    private static String getRequestInfo(HttpServletRequest request) {
        StringBuilder info = new StringBuilder(512);
        if (request != null) {
            String uri = (String) request
                    .getAttribute("javax.servlet.error.request_uri");
            String queryString = (String) request
                    .getAttribute("javax.servlet.forward.query_string");
            if (StringUtils.isNotEmpty(queryString)) {
                uri = uri + "?" + queryString;
            }
            info.append("Request information:").append("\nURL: ").append(uri)
                    .append("\nMethod: ").append(request.getMethod()).append(
                    "\nProtocol: ").append(request.getProtocol())
                    .append("\nRemote host: ").append(request.getRemoteHost())
                    .append("\nRemote address: ").append(
                    request.getRemoteAddr()).append("\nRemote port: ")
                    .append(request.getRemotePort()).append("\nRemote user: ")
                    .append(request.getRemoteUser()).append("\nSession ID: ")
                    .append(request.getRequestedSessionId()).append("\nSession user: ")
                    .append(getUserInfo(request)).append("\nRequest headers: ");

            @SuppressWarnings("unchecked")
            Iterator<String> headerNames = new EnumerationIterator(request.getHeaderNames());
            while (headerNames.hasNext()) {
                String headerName = headerNames.next();
                String headerValue = request.getHeader(headerName);
                info.append("\n  ").append(headerName).append(": ").append(
                        headerValue);
            }
        }
        return info.toString();
    }

    protected static String getUserInfo(HttpServletRequest request) {

        JahiaUser user = JCRSessionFactory.getInstance().getCurrentUser();
        if (user == null) {
            try {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    user = (JahiaUser) session.getAttribute(Constants.SESSION_USER);
                }
            } catch (IllegalStateException ex) {
                // ignore it
            }
        }
        String info = user != null ? user.getUsername() : null;

        // last chance: request's user principal
        info = info != null ? info : String.valueOf(request.getUserPrincipal());

        return info;
    }
}
