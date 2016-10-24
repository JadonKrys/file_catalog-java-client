package edu.wisc.icecube.filecatalog;

import java.lang.reflect.InvocationTargetException;

import org.apache.http.client.HttpResponseException;

public class Error extends HttpResponseException {
	private static final long serialVersionUID = -3720078090584241127L;

	public Error(int statusCode, final String message) {
		super(statusCode, message);
	}
	
	public static class BadRequestError extends Error {
		private static final long serialVersionUID = 5210429972589205351L;

		public BadRequestError(final String message) {
			super(400, message);
		}
	}
	
	public static class TooManyRequestsError extends Error {
		private static final long serialVersionUID = -2362174187596699095L;

		public TooManyRequestsError(final String message) {
			super(429, message);
		}
	}
	
	public static class UnspecificServerError extends Error {
		private static final long serialVersionUID = -7219121887051848533L;

		public UnspecificServerError(final String message) {
			super(500, message);
		}
	}
	
	public static class ServiceUnavailableError extends Error {
		private static final long serialVersionUID = 8931048607677009527L;

		public ServiceUnavailableError(final String message) {
			super(403, message);
		}
	}
	
	public static class ConflictError extends Error {
		private static final long serialVersionUID = 3063072571059867687L;

		public ConflictError(final String message) {
			super(409, message);
		}
	}
	
	public static class NotFoundError extends Error {
		private static final long serialVersionUID = -3222850961244509577L;

		public NotFoundError(final String message) {
			super(404, message);
		}
	}
	
	private static Error[] errors = null;
	private static Class<?>[] errorTypes = null;
	
	public static Error errorFactory(int statusCode, final String message) {
		if(null == errors || null == errorTypes) {
			errorTypes = Error.class.getDeclaredClasses();
			errors = new Error[errorTypes.length];
			
			for(int i = 0; i < errorTypes.length; ++i) {
				try {
					errors[i] = (Error)errorTypes[i].getConstructor(String.class).newInstance("");
				} catch (InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException
						| NoSuchMethodException | SecurityException e) {
					errors[i] = null;
					e.printStackTrace();
				}
			}
		}
		
		for(final Error e: errors) {
			if(null != e && statusCode == e.getStatusCode()) {
				try {
					return (Error)e.getClass().getConstructor(String.class).newInstance(message);
				} catch (InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException
						| NoSuchMethodException | SecurityException e1) {
					// Then let's use the Error class...
					e1.printStackTrace();
				}
			}
		}
		
		return new Error(statusCode, message);
	}
}
