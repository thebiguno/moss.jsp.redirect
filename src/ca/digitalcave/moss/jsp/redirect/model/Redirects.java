package ca.digitalcave.moss.jsp.redirect.model;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("redirects")
public class Redirects {

	@XStreamImplicit(itemFieldName="redirect")
	private List<Redirect> redirects;
	
	public List<Redirect> getRedirects() {
		return redirects;
	}
	public void setRedirects(List<Redirect> redirects) {
		this.redirects = redirects;
	}
}
