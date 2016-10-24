package edu.wisc.icecube.filecatalog.gson;

public class BasicMetaData {
	private String mongo_id;
	private String uid;
	
	public BasicMetaData(final String mongoId, final String uid) {
		this.mongo_id = mongoId;
		this.uid = uid;
	}
	
	public String getMongoId() {
		return mongo_id;
	}
	
	public String getUid() {
		return uid;
	}
}
