/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.hackeriet.struts1Spring.struts;

import org.apache.struts.action.ActionServlet;
import org.apache.struts.actions.LookupDispatchAction;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletContext;
import java.io.File;

/**
 * Convenience class for Spring-aware Struts 1.1+ LookupDispatchActions.
 *
 * <p>Provides a reference to the current Spring application context, e.g.
 * for bean lookup or resource loading. Auto-detects a ContextLoaderPlugIn
 * context, falling back to the root WebApplicationContext. For typical
 * usage, i.e. accessing middle tier beans, use a root WebApplicationContext.
 *
 * <p>For classic Struts Actions, DispatchActions or MappingDispatchActions,
 * use the analogous {@link ActionSupport ActionSupport} or
 * {@link DispatchActionSupport DispatchActionSupport} /
 * {@link MappingDispatchActionSupport MappingDispatchActionSupport} class.
 *
 * <p>As an alternative approach, you can wire your Struts Actions themselves
 * as Spring beans, passing references to them via IoC rather than looking
 * up references in a programmatic fashion. Check out
 * {@link DelegatingActionProxy DelegatingActionProxy} and
 * {@link DelegatingRequestProcessor DelegatingRequestProcessor}.
 *
 * @author Juergen Hoeller
 * @see ContextLoaderPlugIn#SERVLET_CONTEXT_PREFIX
 * @see WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
 * @see org.springframework.web.context.ContextLoaderListener
 * @see ActionSupport
 * @see DispatchActionSupport
 * @see MappingDispatchActionSupport
 * @see DelegatingActionProxy
 * @see DelegatingRequestProcessor
 * @since 1.1
 */
public abstract class LookupDispatchActionSupport extends LookupDispatchAction {

    private WebApplicationContext webApplicationContext;

    private MessageSourceAccessor messageSourceAccessor;


    /**
     * Initialize the WebApplicationContext for this Action.
     * Invokes onInit after successful initialization of the context.
     *
     * @see #initWebApplicationContext
     * @see #onInit
     */
    @Override
    public void setServlet(ActionServlet actionServlet) {
        super.setServlet(actionServlet);
        if (actionServlet != null) {
            this.webApplicationContext = initWebApplicationContext(actionServlet);
            this.messageSourceAccessor = new MessageSourceAccessor(this.webApplicationContext);
            onInit();
        } else {
            onDestroy();
        }
    }

    /**
     * Fetch ContextLoaderPlugIn's WebApplicationContext from the ServletContext,
     * falling back to the root WebApplicationContext (the usual case).
     *
     * @param actionServlet the associated ActionServlet
     * @return the WebApplicationContext
     * @throws IllegalStateException if no WebApplicationContext could be found
     * @see DelegatingActionUtils#findRequiredWebApplicationContext
     */
    protected WebApplicationContext initWebApplicationContext(ActionServlet actionServlet)
            throws IllegalStateException {

        return DelegatingActionUtils.findRequiredWebApplicationContext(actionServlet, null);
    }


    /**
     * Return the current Spring WebApplicationContext.
     * @return the current Spring WebApplicationContext
     */
    protected final WebApplicationContext getWebApplicationContext() {
        return this.webApplicationContext;
    }

    /**
     * Return a MessageSourceAccessor for the application context
     * used by this object, for easy message access.
     * @return a MessageSourceAccessor for the application context
     */
    protected final MessageSourceAccessor getMessageSourceAccessor() {
        return this.messageSourceAccessor;
    }

    /**
     * Return the current ServletContext.
     * @return the current ServletContext.
     */
    protected final ServletContext getServletContext() {
        return this.webApplicationContext.getServletContext();
    }

    /**
     * Return the temporary directory for the current web application,
     * as provided by the servlet container.
     *
     * @return the File representing the temporary directory
     */
    protected final File getTempDir() {
        return WebUtils.getTempDir(getServletContext());
    }


    /**
     * Callback for custom initialization after the context has been set up.
     *
     * @see #setServlet
     */
    protected void onInit() {
    }

    /**
     * Callback for custom destruction when the ActionServlet shuts down.
     *
     * @see #setServlet
     */
    protected void onDestroy() {
    }

}
