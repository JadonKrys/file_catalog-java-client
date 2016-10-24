package edu.wisc.icecube.filecatalog.gson;

public class Links {
	private Link self;
	private Link parent;
	
	public Links(final Link self, final Link parent) {
		this.self = self;
		this.parent = parent;
	}
	
	public Link getSelf() {
		return self;
	}
	
	public Link getParent() {
		return parent;
	}
}
