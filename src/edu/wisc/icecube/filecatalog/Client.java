package edu.wisc.icecube.filecatalog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

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

import edu.wisc.icecube.filecatalog.gson.FileList;

public class Client {
	protected URI uri;
	protected final Gson gson;
	
	public Client(final URI uri) throws URISyntaxException {
		this.uri = joinURIs(uri, "api");
		this.gson = new Gson();
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
	 * @param uri The URI with all parameters
	 * @return The server response represented in {@link FileList} 
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws Error Any error that has the server reported
	 */
	protected FileList getList(final URI uri) throws ClientProtocolException, IOException, Error {
		return gson.fromJson(Request.Get(uri)
									.execute()
									.handleResponse(new ResponseHandleBuilder(HttpStatus.SC_OK)),
							 FileList.class);
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
	 * @return Response of server
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public String create(final String metadata) throws ClientProtocolException, IOException, URISyntaxException, Error {
		if(null == metadata || 0 == metadata.length()) {
			throw new IllegalArgumentException("No metadata given");
		}
		
		return Request.Post(joinURIs(this.uri, "files"))
								  .bodyString(metadata, ContentType.APPLICATION_JSON)
							      .execute()
							      .handleResponse(new ResponseHandleBuilder(HttpStatus.SC_CREATED));
	}
	
	public void get() {
		// TODO
	}
	
	public void update() {
		// TODO
	}
	
	public void replace() {
		// TODO
	}
	
	/**
	 * Deletes metadata by `mongo_id`.
	 * 
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
	
	protected class ResponseHandleBuilder implements ResponseHandler<String> {
		private int goodResponseCode;
		
		public ResponseHandleBuilder(int goodResponseCode) {
			this.goodResponseCode = goodResponseCode;
		}
		
		public String readContent(final HttpEntity entity) throws UnsupportedOperationException, IOException {
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
		
		public String handleResponse(final HttpResponse response) throws IOException {
			final StatusLine statusLine = response.getStatusLine();
			final HttpEntity entity = response.getEntity();
			final String serverResponseString = readContent(entity);
			
			if(statusLine.getStatusCode() == this.goodResponseCode) {
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
