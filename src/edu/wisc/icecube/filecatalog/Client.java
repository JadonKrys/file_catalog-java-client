package edu.wisc.icecube.filecatalog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import edu.wisc.icecube.filecatalog.Error.ConflictError;
import edu.wisc.icecube.filecatalog.Error.NotFoundError;
import edu.wisc.icecube.filecatalog.gson.BasicMetaData;
import edu.wisc.icecube.filecatalog.gson.Creation;
import edu.wisc.icecube.filecatalog.gson.FileList;

public class Client {
	protected URI uri;
	protected final Gson gson;
	protected final Cache cache;
	
	public Client(final URI uri) throws URISyntaxException {
		this.uri = joinURIs(uri, "api");
		this.gson = new Gson();
		this.cache = new Cache();
	}
	
	public Client(final String uri) throws URISyntaxException {
		this(new URI(uri));
	}
	
	public Client(final String uri, int port) throws URISyntaxException {
		this(new URIBuilder(uri).setPort(port).build());
	}
	
	/**
	 * Queries the server by using the GET method to get the file list. It supports the
	 * parameters `query` (a JSON style string to constrain the query), `limit` and `start`.
	 * 
	 * Caches automatically the `uid`/`mongo_id` mapping.
	 * 
	 * @param uri The URI with all parameters
	 * @return The server response represented in {@link FileList} 
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws Error Any error that has the server reported
	 */
	protected FileList getList(final URI uri) throws ClientProtocolException, IOException, Error {
		final FileList list = gson.fromJson(Request.Get(uri)
												.execute()
												.handleResponse(new ResponseHandleBuilder(HttpStatus.SC_OK)),
										    FileList.class);
		
		for(BasicMetaData mapping: list.getEmbedded().getFiles()) {
			cache.setMongoId(mapping.getUid(), mapping.getMongoId());
		}
		
		return list;
	}
	
	/**
	 * Lists the files.
	 * 
	 * @return The server response represented in {@link FileList} 
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public FileList getList() throws ClientProtocolException, IOException, URISyntaxException, Error {
		return getList(null, null, null);
	}
	
	/**
	 * Lists the files by using the `query` parameter.
	 * 
	 * @see #getList()
	 * @see #getList(Integer, Integer)
	 * @see #getList(String, Integer, Integer)
	 * @param The server response represented in {@link FileList} 
	 * @return JSON style string (response from server)
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public FileList getList(final String query) throws ClientProtocolException, IOException, URISyntaxException, Error {
		return getList(query, null, null);
	}
	
	/**
	 * Lists the files by adding the `limit` and `start` parameter.
	 * 
	 * @see #getList()
	 * @see #getList(String)
	 * @see #getList(String, Integer, Integer)
	 * @param limit Limits the number of returned files
	 * @param start Offset of the file list
	 * @return The server response represented in {@link FileList} 
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public FileList getList(final Integer limit, final Integer start) throws ClientProtocolException, IOException, URISyntaxException, Error {
		return getList(null, limit, start);
	}
	
	/**
	 * List the files by adding the `query`, `start` and `limit` parameter. 
	 * 
	 * @param query
	 * @param limit
	 * @param start
	 * @return The server response represented in {@link FileList} 
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public FileList getList(final String query, final Integer limit, final Integer start) throws ClientProtocolException, IOException, URISyntaxException, Error {
		final URIBuilder uri = new URIBuilder(joinURIs(this.uri, "files"));
		
		if(null != query) {
			uri.setParameter("query", query);
		}
		
		if(null != limit) {
			uri.setParameter("limit", limit.toString());
		}
		
		if(null != start) {
			uri.setParameter("start", start.toString());
		}

		return getList(uri.build());
	}
	
	/**
	 * Tries to create a new entry of metadata. Check sever documentation for mandatory/forbidden fields.
	 * 
	 * @param metadata JSON style string
	 * @return Response of server represented as {@link Creation}
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public Creation create(final String metadata) throws ClientProtocolException, IOException, URISyntaxException, Error {
		if(null == metadata || metadata.isEmpty()) {
			throw new IllegalArgumentException("No metadata given");
		}
		
		final Creation creation = gson.fromJson(Request.Post(joinURIs(this.uri, "files"))
											 		   .bodyString(metadata, ContentType.APPLICATION_JSON)
											 	       .execute()
											 	       .handleResponse(new ResponseHandleBuilder(HttpStatus.SC_CREATED)),
											 	Creation.class);
		
		final LinkedTreeMap<?, ?> md = (LinkedTreeMap<?, ?>) gson.fromJson(metadata, Object.class);
		
		// Cache `uid`/`mongo_id`
		cache.setMongoId(findUid(md), getMongoIdFromPath(creation.getFile()));
		
		return creation;
	}
	
	/**
	 * Queries the metadata for the given `mongo_id`.
	 * 
	 * @param mongoId
	 * @return
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public LinkedTreeMap<?, ?> get(final String mongoId) throws ClientProtocolException, UnsupportedEncodingException, IOException, URISyntaxException {
		final ResponseHandleBuilder rhandler = new ResponseHandleBuilder(HttpStatus.SC_OK, true);
		
		final LinkedTreeMap<?, ?> metadata = (LinkedTreeMap<?, ?>) 
				gson.fromJson(Request.Get(joinURIs(this.uri, "files", URLEncoder.encode(mongoId, "UTF-8")))
									 .execute()
									 .handleResponse(rhandler),
					          Object.class);
		
		// Cache etag
		cache.setEtag(mongoId, rhandler.getEtag());
		
		// Cache `uid`/`mongo_id`
		cache.setMongoId(findUid(metadata), mongoId);
		
		return metadata;
	}
	
	/**
	 * Queries the metadata for the given `uid`.
	 * 
	 * @param uid
	 * @return
	 * @throws Error
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws ClientException If the `uid` cannot be mapped to a `mongo_id`.
	 */
	public LinkedTreeMap<?, ?> getByUid(final String uid) throws Error, ClientProtocolException, IOException, URISyntaxException, ClientException {
		return get(getMongoIdByUid(uid));
	}
	
	/**
	 * Updates the metadata by `mongo_id`.
	 * 
	 * <b>Etag:</b> If the metadata with this `mongo_id` has already been queried with this instance, the etag
	 * has been cached and will be used for updating. If there is no Etag in the cache, {@link #get(String)}
	 * will be executed in order to get the Etag.
	 * 
	 * @param mongoId The `mongo_id` identifier for the metadata
	 * @param metadata The metadata as JSON string. E.g. <code>{"backup": True}</code>
	 * @return The updated metadata
	 * @throws ConflictError If the Etag does not match. Try clearing the cache by using {@link #update(String, String, boolean)}.
	 * @throws NotFoundError If no metadata by the given `mongo_id` has been found.
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public LinkedTreeMap<?, ?> update(final String mongoId, final String metadata) throws ClientProtocolException, UnsupportedEncodingException, IOException, URISyntaxException {
		return update(mongoId, metadata, false);
	}
	
	/**
	 * Updates the metadata by `mongo_id`.
	 * 
	 * <b>Etag:</b> If the metadata with this `mongo_id` has already been queried with this instance, the etag
	 * has been cached and will be used for updating. If there is no Etag in the cache, {@link #get(String)}
	 * will be executed in order to get the Etag. If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * 
	 * @param mongoId The `mongo_id` identifier for the metadata
	 * @param metadata The metadata as JSON string. E.g. <code>{"backup": True}</code>
	 * @param clearCache If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * @return The updated metadata
	 * @throws ConflictError If the Etag does not match. Try clearing the cache by using {@link #update(String, String, boolean)}.
	 * @throws NotFoundError If no metadata by the given `mongo_id` has been found.
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public LinkedTreeMap<?, ?> update(final String mongoId, final String metadata, final boolean clearCache) throws ClientProtocolException, UnsupportedEncodingException, IOException, URISyntaxException {
		return updateOrReplace(mongoId, metadata, clearCache, Operation.UPDATE);
	}
	
	/**
	 * Updates the metadata by `uid`.
	 * 
	 * <b>Etag:</b> If the metadata with this `mongo_id` has already been queried with this instance, the etag
	 * has been cached and will be used for updating. If there is no Etag in the cache, {@link #get(String)}
	 * will be executed in order to get the Etag.
	 * 
	 * @param uid The `uid`.
	 * @param metadata The metadata as JSON string. E.g. <code>{"backup": True}</code>
	 * @return The updated metadata
	 * @throws ConflictError If the Etag does not match. Try clearing the cache by using {@link #update(String, String, boolean)}.
	 * @throws NotFoundError If no metadata by the given `mongo_id` has been found.
	 * @throws Error
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws ClientException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public LinkedTreeMap<?, ?> updateByUid(final String uid, final String metadata) throws Error, ClientProtocolException, UnsupportedEncodingException, ClientException, IOException, URISyntaxException {
		return updateByUid(uid, metadata, false);
	}
	
	/**
	 * Updates the metadata by `uid`.
	 * 
	 * <b>Etag:</b> If the metadata with this `mongo_id` has already been queried with this instance, the etag
	 * has been cached and will be used for updating. If there is no Etag in the cache, {@link #get(String)}
	 * will be executed in order to get the Etag. If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * 
	 * @param uid The `uid`.
	 * @param metadata The metadata as JSON string. E.g. <code>{"backup": True}</code>
	 * @param clearCache If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * @return The updated metadata
	 * @throws ConflictError If the Etag does not match. Try clearing the cache by using {@link #update(String, String, boolean)}.
	 * @throws NotFoundError If no metadata by the given `mongo_id` has been found.
	 * @throws Error
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws ClientException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public LinkedTreeMap<?, ?> updateByUid(final String uid, final String metadata, final boolean clearCache) throws Error, ClientProtocolException, UnsupportedEncodingException, ClientException, IOException, URISyntaxException {
		return update(getMongoIdByUid(uid), metadata, clearCache);
	}
	
	/**
	 * Replaces the metadata by `mongo_id`. That means, only the data that is passed with the <code>metadata</code>
	 * will be written into the database. `mongo_id` and `uid` will be added automatically by the server.
	 * 
	 * <b>Etag:</b> If the metadata with this `mongo_id` has already been queried with this instance, the etag
	 * has been cached and will be used for updating. If there is no Etag in the cache, {@link #get(String)}
	 * will be executed in order to get the Etag.
	 * 
	 * @param mongoId The `mongo_id` identifier for the metadata
	 * @param metadata The metadata as JSON string. E.g. <code>{"backup": True}</code>
	 * @return The updated metadata
	 * @throws ConflictError If the Etag does not match. Try clearing the cache by using {@link #update(String, String, boolean)}.
	 * @throws NotFoundError If no metadata by the given `mongo_id` has been found.
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public LinkedTreeMap<?, ?> replace(final String mongoId, final String metadata) throws ClientProtocolException, UnsupportedEncodingException, IOException, URISyntaxException {
		return replace(mongoId, metadata, false);
	}
	
	/**
	 * Replaces the metadata by `mongo_id`. That means, only the data that is passed with the <code>metadata</code>
	 * will be written into the database. `mongo_id` and `uid` will be added automatically by the server.
	 * 
	 * <b>Etag:</b> If the metadata with this `mongo_id` has already been queried with this instance, the etag
	 * has been cached and will be used for updating. If there is no Etag in the cache, {@link #get(String)}
	 * will be executed in order to get the Etag. If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * 
	 * @param mongoId The `mongo_id` identifier for the metadata
	 * @param metadata The metadata as JSON string. E.g. <code>{"backup": True}</code>
	 * @param clearCache If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * @return The updated metadata
	 * @throws ConflictError If the Etag does not match. Try clearing the cache by using {@link #update(String, String, boolean)}.
	 * @throws NotFoundError If no metadata by the given `mongo_id` has been found.
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public LinkedTreeMap<?, ?> replace(final String mongoId, final String metadata, final boolean clearCache) throws ClientProtocolException, UnsupportedEncodingException, IOException, URISyntaxException {
		return updateOrReplace(mongoId, metadata, clearCache, Operation.REPLACE);
	}
	
	/**
	 * Replaces the metadata by `mongo_id`. That means, only the data that is passed with the <code>metadata</code>
	 * will be written into the database. `mongo_id` and `uid` will be added automatically by the server.
	 * 
	 * <b>Etag:</b> If the metadata with this `mongo_id` has already been queried with this instance, the etag
	 * has been cached and will be used for updating. If there is no Etag in the cache, {@link #get(String)}
	 * will be executed in order to get the Etag.
	 * 
	 * @param uid The `uid`.
	 * @param metadata The metadata as JSON string. E.g. <code>{"backup": True}</code>
	 * @return The updated metadata
	 * @throws ConflictError If the Etag does not match. Try clearing the cache by using {@link #update(String, String, boolean)}.
	 * @throws NotFoundError If no metadata by the given `mongo_id` has been found.
	 * @throws Error
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws ClientException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public LinkedTreeMap<?, ?> replaceByUid(final String uid, final String metadata) throws Error, ClientProtocolException, UnsupportedEncodingException, ClientException, IOException, URISyntaxException {
		return replaceByUid(uid, metadata, false);
	}
	
	/**
	 * Replaces the metadata by `mongo_id`. That means, only the data that is passed with the <code>metadata</code>
	 * will be written into the database. `mongo_id` and `uid` will be added automatically by the server.
	 * 
	 * <b>Etag:</b> If the metadata with this `mongo_id` has already been queried with this instance, the etag
	 * has been cached and will be used for updating. If there is no Etag in the cache, {@link #get(String)}
	 * will be executed in order to get the Etag. If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * 
	 * @param uid The `uid`.
	 * @param metadata The metadata as JSON string. E.g. <code>{"backup": True}</code>
	 * @param clearCache If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * @return The updated metadata
	 * @throws ConflictError If the Etag does not match. Try clearing the cache by using {@link #update(String, String, boolean)}.
	 * @throws NotFoundError If no metadata by the given `mongo_id` has been found.
	 * @throws Error
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws ClientException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public LinkedTreeMap<?, ?> replaceByUid(final String uid, final String metadata, final boolean clearCache) throws Error, ClientProtocolException, UnsupportedEncodingException, ClientException, IOException, URISyntaxException {
		return replace(getMongoIdByUid(uid), metadata, clearCache);
	}
	
	/**
	 * Represents the operation that should be executed in {@link Client#updateOrReplace(String, String, boolean, Operation)}.
	 */
	protected enum Operation {UPDATE, REPLACE};

	/**
	 * Since the REPLACE and UPDATE operation only differ in the Http method (PUT, PATCH),
	 * we only need one method that can by used bu the update and replace methods.
	 * 
	 * <b>Etag:</b> If the metadata with this `mongo_id` has already been queried with this instance, the etag
	 * has been cached and will be used for updating. If there is no Etag in the cache, {@link #get(String)}
	 * will be executed in order to get the Etag. If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * 
	 * @param mongoId The `mongo_id` identifier for the metadata
	 * @param metadata The metadata as JSON string. E.g. <code>{"backup": True}</code>
	 * @param clearCache If <code>clearCache</code> is <code>true</code>, the {@link #get(String)}
	 * will be executed to get the latest Etag.
	 * @param operation {@link Operation#REPLACE} or {@link Operation#UPDATE}.
	 * @return The updated metadata
	 * @throws ConflictError If the Etag does not match. Try clearing the cache by using {@link #update(String, String, boolean)}.
	 * @throws NotFoundError If no metadata by the given `mongo_id` has been found.
	 * @throws ClientProtocolException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	protected LinkedTreeMap<?, ?> updateOrReplace(final String mongoId, final String metadata, final boolean clearCache, final Operation operation) throws ClientProtocolException, UnsupportedEncodingException, IOException, URISyntaxException {
		if(null == metadata || metadata.isEmpty()) {
			throw new IllegalArgumentException("No metadata given");
		}
		
		// If clearCache is set to true, it will be forced that
		// the etag will be queried from the server and not be
		// get from the cache
		if(clearCache) {
			cache.deleteEtag(mongoId);
		}
		
		String etag = cache.getEtag(mongoId);
		if(null == etag) {
			// OK, no etag in cache. Use the get() method.
			// There is no need to interpret the result since get()
			// Caches the etag automatically
			get(mongoId);
			
			// Let's try again...
			etag = cache.getEtag(mongoId);
			if(null == etag) {
				throw new ClientException("Could not update file with `mongo_id` = " + mongoId + " because we could not find the etag");
			}
		}
		
		// Let's see... we have the mongo_id, metadata, and the etag... everything we need we have. Yay!
		Request request = null;
		final URI uri = joinURIs(this.uri, "files", URLEncoder.encode(mongoId, "UTF-8"));
		
		switch(operation) {
			case REPLACE:
				request = Request.Put(uri);
				break;
				
			case UPDATE:
				request = Request.Patch(uri);
				break;
				
			default:
				throw new UnsupportedOperationException("Operation " + operation + " is not supported.");
		}
		
		final ResponseHandleBuilder rhandler = new ResponseHandleBuilder(HttpStatus.SC_OK, true);
		
		final String result = request.bodyString(metadata, ContentType.APPLICATION_JSON)
								     .addHeader("If-None-Match", etag)
								     .execute()
								     .handleResponse(rhandler);
		
		// Cache etag
		cache.setEtag(mongoId, rhandler.getEtag());
		
		return (LinkedTreeMap<?, ?>) gson.fromJson(result, Object.class);
	}
	
	/**
	 * Deletes metadata by `mongo_id`.
	 * 
	 * @see #deleteByUid(String)
	 * @param mongoId The `mongo_id`.
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void delete(final String mongoId) throws ClientProtocolException, IOException, URISyntaxException {
		if(null == mongoId || 0 == mongoId.length()) {
			throw new IllegalArgumentException("No mongo_id given");
		}
		
		Request.Delete(joinURIs(this.uri, "files", URLEncoder.encode(mongoId, "UTF-8")))
								.execute()
								.handleResponse(new ResponseHandleBuilder(HttpStatus.SC_NO_CONTENT));
		
		cache.clearCacheByMongoId(mongoId);
	}
	
	/**
	 * Deletes the metadata by `uid`.
	 * 
	 * @see #delete(String)
	 * @param uid
	 * @throws Error
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws ClientException 
	 */
	public void deleteByUid(final String uid) throws Error, ClientProtocolException, IOException, URISyntaxException, ClientException {
		delete(getMongoIdByUid(uid));
	}
	
	/**
	 * Tries to find the corresponding `mongo_id` for the given `uid`.
	 * 
	 * @param uid
	 * @return
	 * @throws Error
	 * @throws ClientProtocolException If no `mongo_id` has been found.
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws ClientException 
	 */
	protected String getMongoIdByUid(final String uid) throws Error, ClientProtocolException, IOException, URISyntaxException, ClientException {
		String mongoId = cache.getMongoId(uid);
		
		if(null == mongoId) {
			// OK, mongo_id isn't in the cache. Query it
			getList("{\"uid\": \"" + uid + "\"}");
			// We don't need to handle the output since getList() caches uid/mongo_id
			
			mongoId = cache.getMongoId(uid);
			if(null == mongoId) {
				throw new ClientException("The uid `" + uid +"` is not present in the file catalog");
			}
		}
		
		return mongoId;
	}
	
	/**
	 * Returns the `uid` that is found in the metadata responded by the server.
	 * 
	 * @param metadata
	 * @return
	 * @throws ClientException
	 */
	protected String findUid(final LinkedTreeMap<?, ?> metadata) throws ClientException {
		if(metadata.containsKey("uid")) {
			return metadata.get("uid").toString();
		} else {
			throw new ClientException("Cannot find `uid` in server response.");
		}
	}
	
	/**
	 * Joins URIs.
	 * 
	 * <b>Example:</b>
	 * <pre>System.out.println(joinURIs(new URI("http://example.com"),
	 * 		new URI("api/"), 
	 * 		new URI("/files"))
	 * );
	 * 
	 * // prints: http://example.com/api/files</pre>
	 * 
	 * @param base
	 * @param uris
	 * @return
	 * @throws URISyntaxException
	 */
	public static URI joinURIs(final URI base, final URI... uris) throws URISyntaxException {
		final String[] paths = new String[uris.length];
		
		for(int i = 0; i < uris.length; ++i) {
			paths[i] = uris[i].getPath();
		}
		
		return joinURIs(base, paths);
	}
	
	/**
	 * Joins URIs.
	 * 
	 * <b>Example:</b>
	 * <pre>System.out.println(joinURIs(new URI("http://example.com"),
	 * 		"api/",  "/files")
	 * );
	 * 
	 * // prints: http://example.com/api/files</pre>
	 * 
	 * @param base
	 * @param uris
	 * @return
	 * @throws URISyntaxException
	 */
	public static URI joinURIs(final URI base, final String... uris) throws URISyntaxException {
		if(null == base) {
			throw new IllegalArgumentException("At least one URI must be passed");
		}
		
		URI uri = base;
		
		for(String u: uris) {
			final char lastChar = uri.toString().charAt(uri.toString().length() - 1);
			final char firstChar = u.charAt(0);
			
			if(lastChar != '/' && firstChar != '/') {
				uri = new URI(uri.toString() + "/" + u);
			} else if(lastChar == '/' && firstChar == '/') {
				uri = new URI(uri.toString() + u.substring(1));
			} else {
				uri = new URI(uri.toString() + u);
			}
		}
		
		return uri;
	}
	
	/**
	 * The server response might contain a list of `files`, or a `file` that is actually a
	 * path how one can get detailed information from the server for a specific `mongo_id`:
	 * `/api/files/580fa1973a7d492ef5a367da`.
	 * 
	 * To obtain the last part (that is te `mongo_id`) one can use this method.
	 * 
	 * @throws IllegalArgumentException An IllegalArgumentException is thrown when no `mongo_id` could be found in the path.
	 * @param path
	 * @return
	 */
	public static String getMongoIdFromPath(final String path) {
		int lastSep = path.lastIndexOf('/');
		
		if(-1 == lastSep || lastSep == path.length() - 1) {
			throw new IllegalArgumentException("The given path does not look like as expected.");
		}
		
		return path.substring(lastSep + 1);
	}
	
	protected class ResponseHandleBuilder implements ResponseHandler<String> {
		private int goodResponseCode;
		private String etag;
		private boolean etagRequired;
		
		public ResponseHandleBuilder(int goodResponseCode) {
			this(goodResponseCode, false);
		}
		
		public ResponseHandleBuilder(int goodResponseCode, boolean etagRequired) {
			this.goodResponseCode = goodResponseCode;
			this.etagRequired = etagRequired;
		}
		
		public String getEtag() {
			return etag;
		}
		
		private String readContent(final HttpEntity entity) throws UnsupportedOperationException, IOException {
			if(null == entity) {
				return null;
			}
			
			final ContentType contentType = ContentType.getOrDefault(entity);
			final Charset charset = contentType.getCharset();
			final Reader reader = new InputStreamReader(entity.getContent(), charset);
			
			final StringBuilder sb = new StringBuilder();
			final char[] buffer = new char[1024];
			
			int rcs = reader.read(buffer, 0, buffer.length);
			while(rcs >= 0) {
				sb.append(buffer, 0, rcs);
				rcs = reader.read(buffer, 0, buffer.length);
			}
			
			reader.close();
			
			return sb.toString();
		}
		
		@Override
		public String handleResponse(final HttpResponse response) throws IOException {
			final StatusLine statusLine = response.getStatusLine();
			final HttpEntity entity = response.getEntity();
			final String serverResponseString = readContent(entity);
			
			if(statusLine.getStatusCode() == this.goodResponseCode) {
				// Find Etag
				final Header header = response.getFirstHeader("etag");
				
				if(etagRequired && null == header) {
					throw new ClientException("The server responded without an etag");
				} else if(null != header) {
					etag = header.getValue();
					
					if(etagRequired && null == etag) {
						throw new ClientException("The server responded without an etag");
					}
				}
				
				return serverResponseString;
			} else {
				if (null == entity) {
		            throw new ClientProtocolException("Response contains no content");
		        } else {
		        	throw Error.errorFactory(statusLine, serverResponseString);
		        }
			}
		}
	}
}
