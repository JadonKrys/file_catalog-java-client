package edu.wisc.icecube.filecatalog.gson;

public class Embedded {
	private BasicMetaData[] files;
	
	public Embedded(final BasicMetaData[] files) {
		this.files = files;
	}
	
	public BasicMetaData[] getFiles() {
		return files;
	}
}
