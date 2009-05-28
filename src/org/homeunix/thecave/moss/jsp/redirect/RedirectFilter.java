/*
 * Created on May 28, 2008 by wyatt
 */
package org.homeunix.thecave.moss.jsp.redirect;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	
	public void init(FilterConfig config) throws ServletException {
		filterConfig = config;
		
		//Try to parse the init param, if available.
		int scheduleInterval;
		try {
			scheduleInterval = Integer.parseInt(filterConfig.getInitParameter("refresh-interval"));

			//Don't let this get less than 5 seconds, for performance reasons
			if (scheduleInterval < 5)
				scheduleInterval = 5;
		}
		catch (NumberFormatException nfe){
			scheduleInterval = 60;
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
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = sf.newSchema(new StreamSource(this.getClass().getResourceAsStream("redirect.xsd")));
			dbf.setSchema(schema);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);
			doc.getDocumentElement().normalize();
			
			//The 'nodes' list now contains all the children of root ('redirects'); each of these should be a 'redirect' element.
			NodeList nodes = doc.getFirstChild().getChildNodes();
			
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeName().equals("redirect")){
					String patternString = node.getAttributes().getNamedItem("pattern").getNodeValue();
					String mapTo = node.getTextContent();
					Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
					redirects.put(pattern, mapTo);
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
				String redirect = matcher.replaceAll(redirects.get(pattern));
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