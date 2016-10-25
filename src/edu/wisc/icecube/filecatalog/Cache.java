package edu.wisc.icecube.filecatalog;

import java.util.HashMap;
import java.util.Map;

public class Cache {
	protected Map<String, String> mongoId;
	
	public Cache() {
		this(20);
	}
	
	public Cache(int initialCapacity) {
		this.mongoId = new HashMap<>(initialCapacity);
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
}
