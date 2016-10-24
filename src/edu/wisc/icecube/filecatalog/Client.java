package edu.wisc.icecube.filecatalog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

public class Client {
	protected URI uri;
	
	public Client(final URI uri) throws URISyntaxException {
		this.uri = joinURIs(uri, "api");
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
	 * @return The server response
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws Error Any error that has the server reported
	 */
	protected String getList(final URI uri) throws ClientProtocolException, IOException, Error {
		return Request.Get(uri).execute().handleResponse(new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response) throws IOException {
		
				final StatusLine statusLine = response.getStatusLine();
				final HttpEntity entity = response.getEntity();
				
				if(statusLine.getStatusCode() == HttpStatus.SC_OK) {
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
				} else {
					if (null == entity) {
			            throw new ClientProtocolException("Response contains no content");
			        } else {
			        	throw Error.errorFactory(statusLine.getStatusCode(), statusLine.getReasonPhrase());
			        }
				}
			}
		});
	}
	
	/**
	 * Lists the files.
	 * 
	 * @return JSON style string (response from server)
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public String getList() throws ClientProtocolException, IOException, URISyntaxException, Error {
		return getList(null, null, null);
	}
	
	/**
	 * Lists the files by using the `query` parameter.
	 * 
	 * @see #getList()
	 * @see #getList(Integer, Integer)
	 * @see #getList(String, Integer, Integer)
	 * @param query JSON style string to constrain the query
	 * @return JSON style string (response from server)
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public String getList(final String query) throws ClientProtocolException, IOException, URISyntaxException, Error {
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
	 * @return JSON style string (response from server)
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public String getList(final Integer limit, final Integer start) throws ClientProtocolException, IOException, URISyntaxException, Error {
		return getList(null, limit, start);
	}
	
	/**
	 * List the files by adding the `query`, `start` and `limit` parameter. 
	 * 
	 * @param query
	 * @param limit
	 * @param start
	 * @return JSON style string (response from server)
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws Error Any error that has the server reported
	 */
	public String getList(final String query, final Integer limit, final Integer start) throws ClientProtocolException, IOException, URISyntaxException, Error {
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
	
	public String create(final String metadata) throws ClientProtocolException, IOException, URISyntaxException, Error {
		if(null == metadata || 0 == metadata.length()) {
			throw new IllegalArgumentException("No metadata given");
		}
		
		final Response r = Request.Patch(joinURIs(this.uri, "files"))
								  .bodyString(metadata, ContentType.APPLICATION_JSON)
							      .execute();
		
		if(r.returnResponse().getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			return r.returnContent().asString();
		} else {
			throw Error.errorFactory(r.returnResponse().getStatusLine().getStatusCode(), r.returnContent().asString());
		}
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
	
	public void delete() {
		// TODO
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
}
