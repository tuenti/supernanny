package com.tuenti.supernanny.repo.exceptions;

public class InvalidReqTypeException extends Exception {
	public InvalidReqTypeException() {
		super();
	}

	public InvalidReqTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidReqTypeException(String message) {
		super(message);
	}

	public InvalidReqTypeException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = -4960158364443775092L;

}
