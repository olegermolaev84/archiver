package com.olegermolaev84.archive.core;

import java.io.Serializable;

/**
 * Contains fields necessary to be written into the output stream
 * prior the compressed data in order to be able to unpack the file/directory later
 *
 */
public class PathHeader implements Serializable {
	private static final long serialVersionUID = -9210386026185737616L;
	
	/** Shows is it file or directory */
	private final boolean isRegularFile;
	
	/** Length of compressed data */
	private final int sizeOfData;
	
	/** String with normalized path name */
	private final String pathName;

	/**
	 * Constructor
	 * @param isFile is it file or directory 
	 * @param lengthOfData length of compressed data
	 * @param pathName string with normalized path name
	 */
	public PathHeader(boolean isFile, int lengthOfData, String pathName) {
		this.isRegularFile = isFile;
		this.sizeOfData = lengthOfData;
		this.pathName = pathName;
	}

	/** Returns flag, which shows is it file or directory
	 * @return <code>true</code> if file, otherwise <code>false</code>
	 */
	public boolean isRegularFile() {
		return isRegularFile;
	}

	/** Returns size of compressed data
	 * @return size of compressed data
	 */
	public int getSizeOfData() {
		return sizeOfData;
	}

	/** Returns string with normalized path name
	 * @return string with normalized path name
	 */
	public String getPathName() {
		return pathName;
	}

}
