/**
 * Jahia Enterprise Edition v6
 *
 * Copyright (C) 2002-2009 Jahia Solutions Group. All rights reserved.
 *
 * Jahia delivers the first Open Source Web Content Integration Software by combining Enterprise Web Content Management
 * with Document Management and Portal features.
 *
 * The Jahia Enterprise Edition is delivered ON AN "AS IS" BASIS, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED.
 *
 * Jahia Enterprise Edition must be used in accordance with the terms contained in a separate license agreement between
 * you and Jahia (Jahia Sustainable Enterprise License - JSEL).
 *
 * If you are unsure which license is appropriate for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.taglibs.uicomponents.loginform;

import java.io.IOException;

import org.jahia.taglibs.ValueJahiaTag;
import org.jahia.data.JahiaData;
import org.jahia.params.ProcessingContext;
import org.jahia.params.valves.LoginEngineAuthValveImpl;
import org.apache.log4j.Logger;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Xavier Lawrence
 */
@SuppressWarnings("serial")
public class IsLoginErrorTag extends ValueJahiaTag {
    private static final transient Logger logger = Logger.getLogger(IsLoginErrorTag.class);

    public int doStartTag() {
        final JahiaData jData = getJahiaData();
        final ProcessingContext jParams = getProcessingContext();
        final String valveResult = (String) jParams.getAttribute(LoginEngineAuthValveImpl.VALVE_RESULT);
        if (!jData.gui().isLogged() && valveResult != null && !LoginEngineAuthValveImpl.OK.equals(valveResult)) {
            if (getVar() != null) {
                pageContext.setAttribute(getVar(), valveResult);
            }
            if (getValueID() != null) {
                pageContext.setAttribute(getValueID(), valveResult);
            }
            return EVAL_BODY_BUFFERED;            
        } else {
            return SKIP_BODY;
        }
    }

    public int doAfterBody() {
        final HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        final JahiaData jData = (JahiaData) request.getAttribute("org.jahia.data.JahiaData");
        if (!jData.gui().isLogged()) {
            final JspWriter out = bodyContent.getEnclosingWriter();
            try {
                bodyContent.writeOut(out);
            } catch (IOException ioe) {
                logger.error("Error:", ioe);
            }
        }
        return SKIP_BODY;
    }


    public int doEndTag() throws JspException {
        resetState();
        return EVAL_PAGE;
    }

    @Override
    protected void resetState() {
        super.resetState();
    }
}
