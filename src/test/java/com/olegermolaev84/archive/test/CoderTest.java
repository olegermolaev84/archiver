package com.olegermolaev84.archive.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.olegermolaev84.archive.core.Coder;

public class CoderTest {
	
	private static final String ARCHIVE_FILE_NAME = "./test/archives/archive";
	private static final String[] FILES_NAMES_TO_PACK = {"./test/source/files", 
			"./test/source/file.txt", 
			"./test/source/images"};
	
	@BeforeAll
	public static void createNeccessaryDurectories() throws IOException {
		if(!Files.exists(Paths.get("./test/archives"))) {
			Files.createDirectory(Paths.get("./test/archives"));
		}
	}
	
	@Test
	public void exceptionOnCompressionLevelMoreThen9() throws IOException {
		try(FileOutputStream fos = new FileOutputStream(ARCHIVE_FILE_NAME)) {
			new Coder(FILES_NAMES_TO_PACK, fos)
			.setCompressionLevel(10);
			fail("Exception is not thrown");
		}
		catch(IllegalArgumentException e){
			assertEquals("Compression level is out of range (0-9). Geven value: 10", e.getMessage());
		}
	}
	
	@Test
	public void exceptionOnCompressionLevelLessThen0() throws IOException {
		try(FileOutputStream fos = new FileOutputStream(ARCHIVE_FILE_NAME)) {
			new Coder(FILES_NAMES_TO_PACK, fos)
			.setCompressionLevel(-1);
			fail("Exception is not thrown");
		}
		catch(IllegalArgumentException e){
			assertEquals("Compression level is out of range (0-9). Geven value: -1", e.getMessage());
		}
	}
	
	@Test
	public void tryToEncodeNotExistentFile() throws IOException {
		try(FileOutputStream fos = new FileOutputStream(ARCHIVE_FILE_NAME)) {
			Coder coder = new Coder(new String[] {"not_existent_file", "not_existent_file2"}, fos);
			boolean result = coder.pack();
			assertEquals(false, result);
			assertEquals(coder.getErrorMessage(), "File: not_existent_file does not exist\n"+
					"File: not_existent_file2 does not exist\n");
		}
	}
	
	@Test
	public void successfulPacking() throws IOException {
		if(Files.exists(Paths.get(ARCHIVE_FILE_NAME))) {
			Files.delete(Paths.get(ARCHIVE_FILE_NAME));
		}
		try(FileOutputStream fos = new FileOutputStream(ARCHIVE_FILE_NAME)) {
			Coder coder = new Coder(FILES_NAMES_TO_PACK, fos);
			boolean result = coder.pack();
			assertEquals(true, result);
			assertEquals(true, Files.exists(Paths.get(ARCHIVE_FILE_NAME)));
			assertEquals(true, Files.size(Paths.get(ARCHIVE_FILE_NAME))> 0);
		}
	}
	
	@AfterAll
	public static void removeArchive() throws IOException {
		if(Files.exists(Paths.get(ARCHIVE_FILE_NAME))) {
			Files.delete(Paths.get(ARCHIVE_FILE_NAME));
		}		
	}
}
