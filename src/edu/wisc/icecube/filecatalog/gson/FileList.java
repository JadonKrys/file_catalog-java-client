package edu.wisc.icecube.filecatalog.gson;

public class FileList extends Entity {
	private Embedded _embedded;
	private String[] files;
	
	public FileList(final Links _links, final Embedded _embedded, final String[] files) {
		super(_links);
		this._embedded = _embedded;
		this.files = files;
	}
	
	public Embedded getEmbedded() {
		return _embedded;
	}
	
	public String[] getFiles() {
		return files;
	}
}
