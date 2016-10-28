package edu.wisc.icecube.filecatalog;

import java.io.IOException;

public class ClientException extends IOException {
	private static final long serialVersionUID = 6748150809669608061L;

	public ClientException(final String message) {
		super(message);
	}
}
