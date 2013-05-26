/*
 * Created on May 28, 2008 by wyatt
 */
package ca.digitalcave.moss.jsp.redirect;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.digitalcave.moss.common.LogUtil;
import ca.digitalcave.moss.jsp.redirect.model.Redirect;
import ca.digitalcave.moss.jsp.redirect.model.Redirects;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Implementation of a simple redirect filter.  Allows the following filter init params:
 *  'conf' - file name of the configuration file, relative to WEB-INF; defaults to 'redirect.xml'
 *  'refresh-interval' - how often to invalidate the cached config file (in seconds), which will 
 *      force a reload at the next request.  Defaults to 60 (1 minute); cannot be set less than 5. 
 * @author wyatt
 *
 */
public class RedirectFilter implements Filter {

	private Map<Pattern, String> redirects;

	private FilterConfig filterConfig;
	private long lastConfigReload = 0;
	private long refreshInterval;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public void init(FilterConfig config) throws ServletException {
		filterConfig = config;
		LogUtil.setLogLevel(config.getInitParameter("log-level"));

		//Try to parse the init param, if available.
		int scheduleInterval;
		try {
			scheduleInterval = Integer.parseInt(filterConfig.getInitParameter("refresh-interval"));

			//Don't let this get less than 5 seconds, for performance reasons
			if (scheduleInterval < 5)
				scheduleInterval = 5;
		}
		catch (NumberFormatException nfe){
			scheduleInterval = 600;
		}

		refreshInterval = scheduleInterval * 1000;
	}

	private synchronized void loadFilters(){
		redirects = Collections.synchronizedMap(new LinkedHashMap<Pattern, String>());
		lastConfigReload = System.currentTimeMillis();
		System.out.println("Loading filters");
		try {
			String confFileName = filterConfig.getInitParameter("config");
			if (confFileName == null || confFileName.length() == 0)
				confFileName = "redirect.xml";
			InputStream is = filterConfig.getServletContext().getResourceAsStream("/WEB-INF/" + confFileName);

			XStream xstream = new XStream(new DomDriver());
			xstream.processAnnotations(Redirects.class);

			Object o = xstream.fromXML(is);
			if (o instanceof Redirects){
				Redirects rs = (Redirects) o;
				for (Redirect r : rs.getRedirects()) {
					String patternString = r.getPattern();
					String destination = r.getDestination();
					Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
					redirects.put(pattern, destination);
					logger.fine("Adding redirect " + patternString + " to " + destination);
				}
			}
		}
		catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		//Verify that this is an HttpServletRequest, and ignore those which are not.
		if (!(req instanceof HttpServletRequest)){
			chain.doFilter(req, res);
			return;
		}

		if (lastConfigReload < System.currentTimeMillis() - refreshInterval)
			loadFilters();

		String fullRequest = ((HttpServletRequest) req).getRequestURL().toString();
		for (Pattern pattern : redirects.keySet()) {
			Matcher matcher = pattern.matcher(fullRequest);
			if (matcher.matches()){
				logger.fine("Request " + fullRequest + " matches pattern " + pattern);
				String redirect = matcher.replaceAll(redirects.get(pattern));
				logger.fine("Redirecting to " + redirect);
				((HttpServletResponse) res).sendRedirect(redirect);
				return;
			}
		}

		//Otherwise, just pass the request on
		chain.doFilter(req, res);
	}

	public void destroy() {
	}
}