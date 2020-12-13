package com.olegermolaev84.archive.util;

/**
 * This exception is thrown if the input stream with packed data is corrupted.
 *
 */
public class FileFormatException extends Exception {

	private static final long serialVersionUID = 7964999890445140428L;

	/**
	 * Constructor
	 * @param message message with error description
	 */
	public FileFormatException(String message) {
		super(message);
	}
}
