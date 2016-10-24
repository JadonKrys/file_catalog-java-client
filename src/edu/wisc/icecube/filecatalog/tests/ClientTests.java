package edu.wisc.icecube.filecatalog.tests;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;

import edu.wisc.icecube.filecatalog.Client;
import edu.wisc.icecube.filecatalog.Error;


public class ClientTests {
	public static void main(final String[] args) throws URISyntaxException, ClientProtocolException, IOException {
		Client c = new Client("http://localhost", 8888);
		
		try {
			System.out.println(c.getList());
		} catch (Error e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
	