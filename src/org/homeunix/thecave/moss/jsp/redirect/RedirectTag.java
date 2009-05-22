package org.homeunix.thecave.moss.jsp.redirect;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RedirectTag implements Tag {
	private PageContext pageContext = null;
	private Tag parent = null;
	private String config = "menu.xml"; //Relative to /WEB-INF/
	
	public String getConfig() {
		return config;
	}
	
	public void setConfig(String config) {
		this.config = config;
	}
	
	public void setPageContext(PageContext arg0) {
		this.pageContext = arg0;
	}
	
	public Tag getParent() {
		return parent;
	}
	
	public void setParent(Tag arg0) {
		this.parent = arg0;
	}
	
	public int doStartTag() throws JspException {
		InputStream is = pageContext.getServletContext().getResourceAsStream("/WEB-INF/" + getConfig());
		
		try {
			pageContext.getOut().println("\n\n<div class='menu'>");
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//			Schema schema = sf.newSchema(new StreamSource(RedirectTag.class.getResourceAsStream("menu.xsd")));
//			dbf.setSchema(schema);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);
			doc.getDocumentElement().normalize();
			NodeList nodes = doc.getChildNodes();

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				parseNode(node, true);
			}
		} catch (Exception e) {
			throw new JspException(e);
		}

		return EVAL_BODY_INCLUDE;
	}
	
	private void parseNode(Node node, boolean firstLevel) throws IOException {
		if ("item".equals(node.getNodeName())){
			pageContext.getOut().print("<li>");
			if (node.getAttributes().getNamedItem("link") != null){
				pageContext.getOut().print("<a href='" + node.getAttributes().getNamedItem("link").getTextContent() + "'>");
			}
			pageContext.getOut().print(node.getTextContent());
			if (node.getAttributes().getNamedItem("link") != null){
				pageContext.getOut().print("</a>");
			}
			pageContext.getOut().println("</li>");
		}
		else if ("menu".equals(node.getNodeName())){
			if (!firstLevel)
				pageContext.getOut().println("<li>");
			pageContext.getOut().println("<ul>");
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				Node childNode = nodes.item(i);
				parseNode(childNode, false);
			}
			pageContext.getOut().println("</ul>");
			if (!firstLevel)
				pageContext.getOut().println("</li>");
		}
	}
	
	public int doEndTag() throws JspException {
		try {
			pageContext.getOut().println("</div> <!-- menu -->\n");
		} catch (IOException e) {
			throw new JspException(e);
		}
		return SKIP_BODY;
	}
	
	public void release() {
		pageContext = null;
		parent = null;
	}
}
