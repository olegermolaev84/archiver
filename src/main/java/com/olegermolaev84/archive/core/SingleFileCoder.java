package com.olegermolaev84.archive.core;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * This class is responsible for compressing a given file.
 */
public class SingleFileCoder extends ByteArrayOutputStream {
	/** Path to an existent file with read permissions */
	private final Path path; 
	
	/** Compression level. Value from 0 to 9 */
	private final int compressionLevel;
	
	/** Block of data to be read from the file per one loop */
	private static final int SIZE_OF_BLOCK = 1024;

	/**
	 * Constructor
	 * @param path Path to an existent file with read permissions
	 * @param compressionLevel Compression level. Value from 0 to 9
	 * @throws IllegalArgumentException will be thrown if the file does not exist, does not
	 *           have read permissions or compression level is out of range.
	 * @throws IOException will be thrown in case of IO errors
	 */
	public SingleFileCoder(Path path, int compressionLevel) throws  IllegalArgumentException, IOException {
		super(Files.isRegularFile(path)?((int)Files.size(path)):0);
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("Path: " + path + " does not exist");
		}
		else if (!Files.isReadable(path)) {
			throw new IllegalArgumentException("Path: " + path + " does not have read permissions");
		}
		else if(compressionLevel > 9 || compressionLevel < 0) {
			throw new IllegalArgumentException("Compression level is out of range (0-9). Geven value: " + compressionLevel);
		}
		
		this.compressionLevel = compressionLevel;
		this.path = path;
	}
	
	/**
	 * Compresses file's data and passes it to the byte array.
	 * @throws IOException will be thrown in case of the file access failure
	 */
	public void packFile() throws IOException {
		if(!Files.isRegularFile(path) || Files.size(path) == 0) {
			return;
		}
		
		try (BufferedInputStream bfis = new BufferedInputStream(new FileInputStream(path.toFile()));
			 DeflaterOutputStream dos = new DeflaterOutputStream(this, new Deflater(compressionLevel))) {

			byte[] byteArray;
			boolean eofReached = false;

			while (!eofReached) {
				byteArray = bfis.readNBytes(SIZE_OF_BLOCK);
				if (byteArray.length == 0) {
					break;
				} else if (byteArray.length < SIZE_OF_BLOCK) {
					eofReached = true;
				}

				dos.write(byteArray);
			}
		}
	}
	
	/**
	 * Returns PathHeader class object with data relevant to the path which was  
	 * used to initialize this SingleFileCoder object. 
	 * Should be called after the <code>packFile</code> method.
	 * @return PathHeader class object with data relevant to the path
	 */
	public PathHeader getPathHeader() {
		return new PathHeader(Files.isRegularFile(path), 
				count, // field of the ByteArrayOutputStream class
				path.normalize().toString());
	}
	
	/**
	 * Returns array of compressed data
	 * @return array of compressed data
	 */
	public byte[] getCompressedData() {
		return buf;
	}
	
	@Override
	public String toString() {
		return "SingleFileCoder("+
				"path=" + path.normalize().toString() +
				", sizeOfData=" + count+
				", isFile="+Files.isRegularFile(path) + ")";
	}
}
