package com.olegermolaev84.archive.main;

import java.io.IOException;

import com.olegermolaev84.archive.core.Coder;
import com.olegermolaev84.archive.core.Decoder;

/**
 * Entry point class
 *
 */
public class Archiver {

	/**
	 * Entry point to the program
	 * @param args Array of strings with files and/or directories to be packed. 
	 * Packed data is directed to the standard output stream.<br>
	 * If the array is empty, then the program unpacks files. Packed data is read from the 
	 * standard input stream.
	 */
	public static void main(String[] args) {
		try {
			if (args.length > 0) { // pack mode
				Coder coder = new Coder(args, System.out);
				if(!coder.pack()) {
					System.err.println(coder.getErrorMessage());
				}
			}
			else {// unpack mode
				Decoder decoder = new Decoder(System.in);
				if(!decoder.unpack()) {
					System.err.println(decoder.getErrorMessage());
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
}
