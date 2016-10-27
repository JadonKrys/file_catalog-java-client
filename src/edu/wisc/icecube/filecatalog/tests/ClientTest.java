package edu.wisc.icecube.filecatalog.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import edu.wisc.icecube.filecatalog.Client;
import edu.wisc.icecube.filecatalog.ClientException;
import edu.wisc.icecube.filecatalog.Error;
import edu.wisc.icecube.filecatalog.Error.BadRequestError;
import edu.wisc.icecube.filecatalog.Error.ConflictError;
import edu.wisc.icecube.filecatalog.Error.NotFoundError;
import edu.wisc.icecube.filecatalog.gson.BasicMetaData;
import edu.wisc.icecube.filecatalog.gson.Creation;
import edu.wisc.icecube.filecatalog.gson.FileList;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientTest {
	private static Client client;
	private static String metadata;
	private static String metadataReplica;
	private static String replaceMetadata;
	private static String replaceBadMetadata;
	private static String uid;
	private static String mongoId;
	private static String createdFile;
	private static String updateMetadata;
	private static String updateBadMetadata;
	private static Gson gson;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		client = new Client("http://localhost", 8888);
		
		gson = new Gson();
		
		uid = "__test_java_" + new Date();
		
		final String checksum = getFakeSHA512(42);
		
		metadata = "{'uid': '" + uid + "', 'locations': ['/path/to/file.dat'], 'checksum': '" + checksum + "'}";
		metadata = metadata.replace('\'', '"');
		
		metadataReplica = "{'uid': '" + uid + "', 'locations': ['http://www.example.com/file.dat'], 'checksum': '" + checksum + "'}";
		metadataReplica = metadataReplica.replace('\'', '"');
		
		updateMetadata = "{'backup': true}";
		updateMetadata = updateMetadata.replace('\'', '"');
		
		updateBadMetadata = "{'mongo_id': 'abcd'}";
		updateBadMetadata = updateBadMetadata.replace('\'', '"');
		
		replaceMetadata = "{'locations': ['/path/to/another/file.dat'], 'checksum': '" + checksum + "'}";
		replaceMetadata = replaceMetadata.replace('\'', '"');
		
		replaceBadMetadata = "{'uid': 'E=mc^2', 'locations': ['/path/to/another/file.dat'], 'checksum': '" + checksum + "'}";
		replaceBadMetadata = replaceBadMetadata.replace('\'', '"');
	}

	@Test
	public void testCreate1() throws Error, ClientProtocolException, IOException, URISyntaxException {
		final Creation creation = client.create(metadata);
		
		createdFile = creation.getFile();
		
		mongoId = Client.getMongoIdFromPath(createdFile);
		
		assertNotEquals(createdFile, null);
	}
	
	@Test(expected = ConflictError.class)
	public void testCreate2() throws Error, ClientProtocolException, IOException, URISyntaxException {
		// Add Replica fails
		client.create(metadata);
	}
	
	@Test
	public void testZCreate3() throws Error, ClientProtocolException, IOException, URISyntaxException {
		// Add replica
		final Creation creation = client.create(metadataReplica);
		
		assertEquals(mongoId, Client.getMongoIdFromPath(creation.getFile()));
	}
	
	@Test
	public void testGetList1() throws Error, ClientProtocolException, IOException, URISyntaxException {
		final FileList list = client.getList();
		
		assertNotEquals(list, null);
		
		assertNotEquals(list.getEmbedded(), null);
		assertNotEquals(list.getEmbedded().getFiles(), null);
		assertTrue(list.getEmbedded().getFiles().length >= 1);
		
		// Look for created file
		boolean found = false;
		for(BasicMetaData md: list.getEmbedded().getFiles()) {
			if(md.getMongoId().equals(mongoId) && md.getUid().equals(uid)) {
				found = true;
				break;
			}
		}
		
		assertTrue(found);
		
		// ==========================================================
		assertNotEquals(list.getFiles(), null);
		assertTrue(list.getFiles().length >= 1);
		
		// Look for created file
		found = false;
		for(String file: list.getFiles()) {
			if(file.equals(createdFile)) {
				found = true;
				break;
			}
		}
		
		assertTrue(found);
		
		// ==========================================================
		assertNotEquals(list.getLinks(), null);
		assertNotEquals(list.getLinks().getParent(), null);
		assertEquals(list.getLinks().getParent().getHref(), "/api");
		assertNotEquals(list.getLinks().getSelf(), null);
		assertEquals(list.getLinks().getSelf().getHref(), "/api/files");
	}

	@Test
	public void testGetList2() throws Error, ClientProtocolException, IOException, URISyntaxException {
		final FileList list = client.getList(1, null);
		
		assertEquals(1, list.getFiles().length);
	}
	
	@Test(expected = BadRequestError.class)
	public void testGetList3() throws Error, ClientProtocolException, IOException, URISyntaxException {
		client.getList(-1, null);
	}
	
	@Test(expected = BadRequestError.class)
	public void testGetList4() throws Error, ClientProtocolException, IOException, URISyntaxException {
		client.getList(null, -1);
	}
	
	@Test
	public void testGetList5() throws Error, ClientProtocolException, IOException, URISyntaxException {
		final FileList list = client.getList(("{'uid': '" + uid + "'}").replace('\'', '"'));
		
		assertArrayEquals(new Object[] {createdFile}, list.getFiles());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGet1() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		final LinkedTreeMap<Object, Object> result = (LinkedTreeMap<Object, Object>) client.getByUid(uid);
		final LinkedTreeMap<Object, Object> expectation = (LinkedTreeMap<Object, Object>) gson.fromJson(metadata, Object.class);
		
		expectation.put("mongo_id", mongoId);
		result.remove("_links");
		
		assertEquals(expectation, result);
	}
	
	@Test(expected = ClientException.class)
	public void testGet2() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.getByUid(uid + "does not exist");
	}
	
	@Test(expected = BadRequestError.class)
	public void testGet3() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.get("123");
	}
	
	@Test(expected = NotFoundError.class)
	public void testGet4() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.get("000000000000000000000000");
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdate1() throws Error, ClientProtocolException, UnsupportedEncodingException, ClientException, IOException, URISyntaxException {
		final LinkedTreeMap<Object, Object> updatedMetadata = (LinkedTreeMap<Object, Object>) client.updateByUid(uid, updateMetadata);
		final LinkedTreeMap<Object, Object> expectation = (LinkedTreeMap<Object, Object>) gson.fromJson(metadata, Object.class);
		
		final LinkedTreeMap<Object, Object> update = (LinkedTreeMap<Object, Object>) gson.fromJson(updateMetadata, Object.class);
		
		expectation.put("mongo_id", mongoId);
		expectation.putAll(update);
		updatedMetadata.remove("_links");
		
		assertEquals(expectation, updatedMetadata);
	}
	
	@Test(expected = BadRequestError.class)
	public void testUpdate2() throws Error, ClientProtocolException, UnsupportedEncodingException, ClientException, IOException, URISyntaxException {
		client.updateByUid(uid, updateBadMetadata);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testYReplace1() throws Error, ClientProtocolException, UnsupportedEncodingException, ClientException, IOException, URISyntaxException {
		final LinkedTreeMap<Object, Object> replacedMetadata = (LinkedTreeMap<Object, Object>) client.replaceByUid(uid, replaceMetadata);
		final LinkedTreeMap<Object, Object> expectation = (LinkedTreeMap<Object, Object>) gson.fromJson(replaceMetadata, Object.class);
		
		expectation.put("mongo_id", mongoId);
		expectation.put("uid", uid);
		replacedMetadata.remove("_links");
		
		assertEquals(expectation, replacedMetadata);
	}
	
	@Test(expected = BadRequestError.class)
	public void testYReplace2() throws Error, ClientProtocolException, UnsupportedEncodingException, ClientException, IOException, URISyntaxException {
		client.replaceByUid(uid, replaceBadMetadata);
	}
	
	@Test
	public void testZDelete0() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.deleteByUid(uid);
	}
	
	@Test(expected = ClientException.class)
	public void testZDelete1() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.deleteByUid(uid);
	}
	
	@Test(expected = BadRequestError.class)
	public void testZDelete2() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.delete("bad mongo_id");
	}
	
	@Test(expected = NotFoundError.class)
	public void testZDelete3() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.delete(mongoId);
	}
	
	private static String getFakeSHA512(final long seed) {
		final String validChars = "0123456789abcdef";
		final StringBuilder sb = new StringBuilder(128);
		
		Random rand = new Random();
		rand.setSeed(seed);
		
		for(int i = 0; i < sb.capacity(); ++i) {
			sb.append(validChars.charAt(rand.nextInt(validChars.length())));
		}
		
		return sb.toString();
	}
}
