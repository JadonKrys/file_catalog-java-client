package edu.wisc.icecube.filecatalog.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import edu.wisc.icecube.filecatalog.Client;
import edu.wisc.icecube.filecatalog.ClientException;
import edu.wisc.icecube.filecatalog.Error;
import edu.wisc.icecube.filecatalog.Error.BadRequestError;
import edu.wisc.icecube.filecatalog.Error.NotFoundError;
import edu.wisc.icecube.filecatalog.gson.BasicMetaData;
import edu.wisc.icecube.filecatalog.gson.Creation;
import edu.wisc.icecube.filecatalog.gson.FileList;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientTest {
	private static Client client;
	private static String metadata;
	private static String uid;
	private static String createdFile;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		client = new Client("http://localhost", 8888);
		
		uid = "__test_java_" + new Date();
		
		metadata = "{'uid': '" + uid + "', 'locations': ['/path/to/file.dat'], 'checksum': '" + getFakeSHA512(42) + "'}";
		metadata = metadata.replace('\'', '"');
	}

	@Test
	public void testCreate() throws Error, ClientProtocolException, IOException, URISyntaxException {
		final Creation creation = client.create(metadata);
		
		createdFile = creation.getFile();
		
		assertNotEquals(createdFile, null);
	}
	
	@Test
	public void testGetList1() throws Error, ClientProtocolException, IOException, URISyntaxException {
		final FileList list = client.getList();
		
		System.out.println(list);
		
		assertNotEquals(list, null);
		
		assertNotEquals(list.getEmbedded(), null);
		assertNotEquals(list.getEmbedded().getFiles(), null);
		assertTrue(list.getEmbedded().getFiles().length >= 1);
		
		// Look for created file
		boolean found = false;
		for(BasicMetaData md: list.getEmbedded().getFiles()) {
			if(md.getMongoId().equals(Client.getMongoIdFromPath(createdFile)) && md.getUid().equals(uid)) {
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
	
	@Test(expected = ClientException.class)
	public void testZDelete1() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.deleteByUid(uid + " does not exist");
	}
	
	@Test(expected = NotFoundError.class)
	public void testZDelete2() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.delete("does not exist");
	}
	
	@Test
	public void testZZFinalDelete() throws Error, ClientProtocolException, ClientException, IOException, URISyntaxException {
		client.deleteByUid(uid);
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
