package edu.wisc.icecube.filecatalog.tests;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;

import edu.wisc.icecube.filecatalog.Client;
import edu.wisc.icecube.filecatalog.Error;


public class ClientTests {
	public static void main(final String[] args) throws URISyntaxException, ClientProtocolException, IOException {
		Client c = new Client("http://localhost", 8888);
		
		try {
			System.out.println(c.getList());
			System.out.println(c.create("{\"uid\": \"__test_java\", \"checksum\": \"" + getFakeSHA512(42l) + "\", \"locations\": [\"test.file\"]}"));
			System.out.println(c.getList());
		} catch (Error e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	