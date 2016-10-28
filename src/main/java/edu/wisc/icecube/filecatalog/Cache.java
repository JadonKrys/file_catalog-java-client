package edu.wisc.icecube.filecatalog;

import java.util.HashMap;
import java.util.Map;

public class Cache {
	protected Map<String, String> mongoId;
	protected Map<String, String> etag;
	
	public Cache() {
		this(20);
	}
	
	public Cache(int initialCapacity) {
		this.mongoId = new HashMap<>(initialCapacity);
		this.etag = new HashMap<>(initialCapacity);
	}
	
	/**
	 * Returns the `mongo_id` that is mapped to the given `uid`. If the `uid` is not mapped, `null` is returned.
	 * 
	 * @see Map#get(Object)
	 * @param uid
	 * @return The `mongo_id`.
	 */
	public String getMongoId(final String uid) {
		return this.mongoId.get(uid);
	}
	
	/**
	 * Returns `true` if the `uid` is mapped to a `mongo_id`.
	 * 
	 * @param uid
	 * @return
	 */
	public boolean hasMongoId(final String uid) {
		return this.mongoId.containsKey(uid);
	}
	
	/**
	 * Removes the `mongo_id` from the cache.
	 * 
	 * <b>Note:</b> This method takes the `mongo_id` <b>not</b> the `uid`.
	 * 
	 * @param mongoId
	 */
	public void deleteMongoId(final String mongoId) {
		this.mongoId.values().remove(mongoId);
	}
	
	/**
	 * Sets the pair `uid` and `mongo_id`. If the `uid` already exists, it will be replaced.
	 * 
	 * @see Map#put(Object, Object)
	 * @param uid
	 * @param mongoId
	 */
	public void setMongoId(final String uid, final String mongoId) {
		this.mongoId.put(uid, mongoId);
	}
	
	/**
	 * Clears all mappings that correspond to the given `mongo_id`.
	 * 
	 * @param mongoId
	 */
	public void clearCacheByMongoId(final String mongoId) {
		deleteMongoId(mongoId);
		deleteEtag(mongoId);
	}
	
	/**
	 * Returns the `etag` that is mapped to the given `mongo_id`. If the `mongo_id` is not mapped, `null` is returned.
	 * 
	 * @see Map#get(Object)
	 * @param mongoId
	 * @return The `etag`.
	 */
	public String getEtag(final String mongo_id) {
		return this.etag.get(mongo_id);
	}
	
	/**
	 * Returns `true` if the `mongo_id` is mapped to a `etag`.
	 * 
	 * @param mongoId
	 * @return
	 */
	public boolean hasEtag(final String mongoId) {
		return this.etag.containsKey(mongoId);
	}
	
	/**
	 * Removes the `etag` from the cache.
	 * 
	 * @param mongoId
	 */
	public void deleteEtag(final String mongoId) {
		this.etag.remove(mongoId);
	}
	
	/**
	 * Sets the pair `mongo_id` and `etag`. If the `mongo_id` already exists, it will be replaced.
	 * 
	 * @see Map#put(Object, Object)
	 * @param etag
	 * @param mongoId
	 */
	public void setEtag(final String mongoId, final String etag) {
		this.etag.put(mongoId, etag);
	}
}
