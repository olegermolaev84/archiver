package com.olegermolaev84.archive.core;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.InflaterOutputStream;

/**
 * This class is responsible for unpacking the given data and creating
 * the corresponding file or directory
 */
public class SingleFileDecoder {
	/** Shows what to be created: file or directory */
	private final boolean isFile;
	
	/** Byte array with compressed data */
	private final byte[] compressedData;
	
	/** Path with file or directory to be created */
	private final Path path; 
	
	/**
	 * Constructor
	 * @param isFile Shows what to be created: file or directory
	 * @param compressedData Byte array with compressed data
	 * @param path Path with file or directory to be created
	 */
	public SingleFileDecoder(boolean isFile, byte[] compressedData, Path path) {
		this.isFile = isFile;
		this.compressedData = compressedData;
		this.path = path;
	}
	
	/**
	 * Decompresses the data and creates the corresponding file or directory
	 * @throws IOException will be thrown in case of IO errors
	 */
	public void  unpackFile() throws IOException {
		// create directories
		if(!isFile && !Files.exists(path)) {
			Files.createDirectories(path);
		}
		else if(isFile && !(Files.exists(path.getParent()))) {
			Files.createDirectories(path.getParent());
		}
		
		if(isFile) {
			Files.createFile(path);
		}
		
		// decompress the data and write it to the file
		if(compressedData.length > 0) {
			InflaterOutputStream ios = new InflaterOutputStream(new BufferedOutputStream(new FileOutputStream(path.toFile())));
			ios.write(compressedData);
			ios.close();
		}
	}
	
	/**
	 * Returns normalized name of the path
	 * @return normalized name of the path
	 */
	public String getFileName() {
		return path.normalize().toString();
	}
	
	@Override
	public String toString() {
		return "SimpleFileDecoder(isFile="+isFile+", path="+path+", data length="+compressedData.length+")";
	}
}
