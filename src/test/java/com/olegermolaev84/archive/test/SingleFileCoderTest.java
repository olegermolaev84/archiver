package com.olegermolaev84.archive.test;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.olegermolaev84.archive.core.PathHeader;
import com.olegermolaev84.archive.core.SingleFileCoder;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SingleFileCoderTest {
	
	@Test
	public void exceptionOnNonExistentFile() throws IOException {
		try {
			new SingleFileCoder(Paths.get("not_existent_file"), 9);
			fail("Exception is not thrown");
		}
		catch(IllegalArgumentException e){
			assertEquals("Path: not_existent_file does not exist", e.getMessage());
		}
	}
	
	@Test
	public void exceptionOnCompressionLevelMoreThen9() throws IOException {
		try {
			new SingleFileCoder(Paths.get("./test/source/files/empty.txt"), 10);
			fail("Exception is not thrown");
		}
		catch(IllegalArgumentException e){
			assertEquals("Compression level is out of range (0-9). Geven value: 10", e.getMessage());
		}
	}
	
	@Test
	public void exceptionOnCompressionLevelLessThen0() throws IOException {
		try {
			new SingleFileCoder(Paths.get("./test/source/files/empty.txt"), -5);
			fail("Exception is not thrown");
		}
		catch(IllegalArgumentException e){
			assertEquals("Compression level is out of range (0-9). Geven value: -5", e.getMessage());
		}
	}
	
	@Test
	public void emptyFilePackedSuccessfully() throws IOException {
		SingleFileCoder coder = new SingleFileCoder(Paths.get("./test/source/files/empty.txt"), 9);
		coder.packFile();
		PathHeader header = coder.getPathHeader();
		assertEquals(header.isRegularFile(), true);
		assertEquals(header.getSizeOfData(), 0);
		assertEquals(header.getPathName(), "test\\source\\files\\empty.txt");
	}
	
	@Test
	public void folderPackedSuccessfully() throws IOException {
		SingleFileCoder coder = new SingleFileCoder(Paths.get("./test/source/files"), 9);
		coder.packFile();
		PathHeader header = coder.getPathHeader();
		assertEquals(header.isRegularFile(), false);
		assertEquals(header.getSizeOfData(), 0);
		assertEquals(header.getPathName(), "test\\source\\files");
	}
	
	@Test
	public void notEmptyFilePackedSuccessfully() throws IOException {
		SingleFileCoder coder = new SingleFileCoder(Paths.get("./test/source/file.txt"), 9);
		coder.packFile();
		PathHeader header = coder.getPathHeader();
		assertEquals(header.isRegularFile(), true);
		assertEquals(header.getSizeOfData()>0, true);
		assertEquals(header.getPathName(), "test\\source\\file.txt");
	}
}
