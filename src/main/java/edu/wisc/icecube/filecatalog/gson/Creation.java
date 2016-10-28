package edu.wisc.icecube.filecatalog.gson;

public class Creation extends Entity {
	private String file;
	
	public Creation(final Links _links, final String file) {
		super(_links);
		
		this.file = file;
	}
	
	public String getFile() {
		return file;
	}
}
