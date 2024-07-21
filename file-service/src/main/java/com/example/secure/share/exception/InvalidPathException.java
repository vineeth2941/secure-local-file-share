package com.example.secure.share.exception;

public class InvalidPathException extends IllegalArgumentException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 511771508392094402L;
	
	public InvalidPathException(String path) {
		super("Invalid file path: " + path);
	}

}
