package ca.digitalcave.moss.jsp.redirect.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("redirect")
public class Redirect {

	private String pattern;
	private String destination;

	public String getDestination() {
		return destination;
	}
	public void setDestination(String destination) {
		this.destination = destination;
	}
	public String getPattern() {
		return pattern;
	}
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
}
