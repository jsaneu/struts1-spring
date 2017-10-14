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

package org.springframework.web.struts;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.config.ModuleConfig;
import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Subclass of Struts's default {@link RequestProcessor} that looks up
 * Spring-managed Struts {@link Action Actions} defined in
 * {@link ContextLoaderPlugIn ContextLoaderPlugIn's} {@link WebApplicationContext}
 * (or, as a fallback, in the root {@code WebApplicationContext}).
 *
 * <p>In the Struts config file, you can either specify the original
 * {@code Action} class (as when generated by XDoclet), or no
 * {@code Action} class at all. In any case, Struts will delegate to an
 * {@code Action} bean in the {@code ContextLoaderPlugIn} context.
 *
 * <pre class="code">&lt;action path="/login" type="myapp.MyAction"/&gt;</pre>
 *
 * or
 *
 * <pre class="code">&lt;action path="/login"/&gt;</pre>
 *
 * The name of the {@code Action} bean in the
 * {@code WebApplicationContext} will be determined from the mapping path
 * and module prefix. This can be customized by overriding the
 * {@link #determineActionBeanName} method.
 *
 * <p>Example:
 * <ul>
 * <li>mapping path "/login" -> bean name "/login"<br>
 * <li>mapping path "/login", module prefix "/mymodule" ->
 * bean name "/mymodule/login"
 * </ul>
 *
 * <p>A corresponding bean definition in the {@code ContextLoaderPlugin}
 * context would look as follows; notice that the {@code Action} is now
 * able to leverage fully Spring's configuration facilities:
 *
 * <pre class="code">
 * &lt;bean name="/login" class="myapp.MyAction"&gt;
 *   &lt;property name="..."&gt;...&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Note that you can use a single {@code ContextLoaderPlugIn} for all
 * Struts modules. That context can in turn be loaded from multiple XML files,
 * for example split according to Struts modules. Alternatively, define one
 * {@code ContextLoaderPlugIn} per Struts module, specifying appropriate
 * "contextConfigLocation" parameters. In both cases, the Spring bean name has
 * to include the module prefix.
 *
 * <p>If you also need the Tiles setup functionality of the original
 * {@code TilesRequestProcessor}, use
 * {@code DelegatingTilesRequestProcessor}. As there is just a
 * single central class to customize in Struts, we have to provide another
 * subclass here, covering both the Tiles and the Spring delegation aspect.
 *
 * <p>If this {@code RequestProcessor} conflicts with the need for a
 * different {@code RequestProcessor} subclass (other than
 * {@code TilesRequestProcessor}), consider using
 * {@link DelegatingActionProxy} as the Struts {@code Action} type in
 * your struts-config file.
 *
 * <p>The default implementation delegates to the
 * {@code DelegatingActionUtils} class as much as possible, to reuse as
 * much code as possible despite the need to provide two
 * {@code RequestProcessor} subclasses. If you need to subclass yet
 * another {@code RequestProcessor}, take this class as a template,
 * delegating to {@code DelegatingActionUtils} just like it.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see #determineActionBeanName
 * @see DelegatingTilesRequestProcessor
 * @see DelegatingActionProxy
 * @see DelegatingActionUtils
 * @see ContextLoaderPlugIn
 */
public class DelegatingRequestProcessor extends RequestProcessor {

	private WebApplicationContext webApplicationContext;


	@Override
	public void init(ActionServlet actionServlet, ModuleConfig moduleConfig) throws ServletException {
		super.init(actionServlet, moduleConfig);
		if (actionServlet != null) {
			this.webApplicationContext = initWebApplicationContext(actionServlet, moduleConfig);
		}
	}

	/**
	 * Fetch ContextLoaderPlugIn's {@link WebApplicationContext} from the
	 * {@code ServletContext}, falling back to the root
	 * {@code WebApplicationContext}.
	 * <p>This context is supposed to contain the Struts {@code Action}
	 * beans to delegate to.
	 * @param actionServlet the associated {@code ActionServlet}
	 * @param moduleConfig the associated {@code ModuleConfig}
	 * @return the {@code WebApplicationContext}
	 * @throws IllegalStateException if no {@code WebApplicationContext} could be found
	 * @see DelegatingActionUtils#findRequiredWebApplicationContext
	 * @see ContextLoaderPlugIn#SERVLET_CONTEXT_PREFIX
	 */
	protected WebApplicationContext initWebApplicationContext(
			ActionServlet actionServlet, ModuleConfig moduleConfig) throws IllegalStateException {

		return DelegatingActionUtils.findRequiredWebApplicationContext(actionServlet, moduleConfig);
	}

	/**
	 * Return the {@code WebApplicationContext} that this processor
	 * delegates to.
	 */
	protected final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	/**
	 * Override the base class method to return the delegate action.
	 * @see #getDelegateAction
	 */
	@Override
	protected Action processActionCreate(
			HttpServletRequest request, HttpServletResponse response, ActionMapping mapping)
			throws IOException {

		Action action = getDelegateAction(mapping);
		if (action != null) {
			return action;
		}
		return super.processActionCreate(request, response, mapping);
	}

	/**
	 * Return the delegate {@code Action} for the given mapping.
	 * <p>The default implementation determines a bean name from the
	 * given {@code ActionMapping} and looks up the corresponding
	 * bean in the {@code WebApplicationContext}.
	 * @param mapping the Struts {@code ActionMapping}
	 * @return the delegate {@code Action}, or {@code null} if none found
	 * @throws BeansException if thrown by {@code WebApplicationContext} methods
	 * @see #determineActionBeanName
	 */
	protected Action getDelegateAction(ActionMapping mapping) throws BeansException {
		String beanName = determineActionBeanName(mapping);
		if (!getWebApplicationContext().containsBean(beanName)) {
			return null;
		}
		return getWebApplicationContext().getBean(beanName, Action.class);
	}

	/**
	 * Determine the name of the {@code Action} bean, to be looked up in
	 * the {@code WebApplicationContext}.
	 * <p>The default implementation takes the
	 * {@link org.apache.struts.action.ActionMapping#getPath mapping path} and
	 * prepends the
	 * {@link org.apache.struts.config.ModuleConfig#getPrefix module prefix},
	 * if any.
	 * @param mapping the Struts {@code ActionMapping}
	 * @return the name of the Action bean
	 * @see DelegatingActionUtils#determineActionBeanName
	 * @see org.apache.struts.action.ActionMapping#getPath
	 * @see org.apache.struts.config.ModuleConfig#getPrefix
	 */
	protected String determineActionBeanName(ActionMapping mapping) {
		return DelegatingActionUtils.determineActionBeanName(mapping);
	}

}
