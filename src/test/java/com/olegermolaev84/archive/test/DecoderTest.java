package com.olegermolaev84.archive.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.olegermolaev84.archive.core.Coder;
import com.olegermolaev84.archive.core.Decoder;

public class DecoderTest {
	private final static String ARCHIVE_FILE_NAME = "./test/archives/archive";
	private final static String[] FILES_NAMES_TO_PACK = {"./test/source/files", 
			"./test/source/file.txt", 
			"./test/source/images"};
	private final static Path PARRENT_PATH = Paths.get("./test/output");
	
	@BeforeAll
	public static void createArchive() throws FileNotFoundException, IOException {
		try(FileOutputStream fos = new FileOutputStream(ARCHIVE_FILE_NAME)) {
			new Coder(FILES_NAMES_TO_PACK, fos)
			.pack();
		}	
		
		if(!Files.exists(Paths.get("./test/archives"))) {
			Files.createDirectory(Paths.get("./test/archives"));
		}
		if(!Files.exists(Paths.get("./test/output"))) {
			Files.createDirectory(Paths.get("./test/output"));
		}
	}
	
	@AfterAll
	public static void deleteArchive() throws IOException {
		if(Files.exists(Paths.get(ARCHIVE_FILE_NAME))){
			Files.delete(Paths.get(ARCHIVE_FILE_NAME));
		}
	}
	
	@Test
	public void exceptionOnSettingNonExistentParrentPath() throws IOException  {
		try (FileInputStream is = new FileInputStream(ARCHIVE_FILE_NAME)) {
			new Decoder(is).setParentPath(Paths.get("non-existent"));
			fail("Exception is not thrown");
		}
		catch(IllegalArgumentException e){
			assertEquals("Path: non-existent does not exist", e.getMessage());
		}
	}
	
	@Test
	public void exceptionOnSettingFileInsteadOfDirectoryAsParrentPath() throws IOException  {
		try (FileInputStream is = new FileInputStream(ARCHIVE_FILE_NAME)){
			new Decoder(is).setParentPath(Paths.get("./test/source/file.txt"));
			fail("Exception is not thrown");
		}
		catch(IllegalArgumentException e){
			if(System.getProperty("os.name").contains("Windows")) {
				assertEquals("Path: .\\test\\source\\file.txt is not directory", e.getMessage());
			}else {
				assertEquals("Path: ./test/source/file.txt is not directory", e.getMessage());
			}
		}
	}
	
	@Test
	public void successfulUnpacking() throws IOException, InterruptedException {
		FileInputStream is = new FileInputStream(ARCHIVE_FILE_NAME);
		Decoder decoder = new Decoder(is);
		decoder.setParentPath(PARRENT_PATH);
		boolean result = decoder.unpack();
		assertEquals("", decoder.getErrorMessage());
		assertEquals(true, result);
		Arrays.asList(FILES_NAMES_TO_PACK).stream()
		.forEach((pathName)->{
			try(Stream<Path> paths = Files.walk(Path.of(pathName))){
					paths.forEach((path)->{
						Path outputPath = PARRENT_PATH.resolve(path);
						assertEquals(true, Files.exists(outputPath));
						try {
							assertEquals(Files.size(path), Files.size(outputPath));
						} catch (IOException e) {
							e.printStackTrace();
						}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		is.close();
		cleanOutputFolder();
	}
	
	private void cleanOutputFolder() {
		try (Stream<Path> paths = Files.walk(PARRENT_PATH)) {
			paths.forEach((path) -> {
				try {
					Files.walkFileTree(PARRENT_PATH, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
								throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) 
								throws IOException {
							if (exc != null) {
								throw exc;
							}
							if(!dir.equals(PARRENT_PATH)) {
								Files.delete(dir);
							}
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void errorOnAttemtToUnpackFileWhichAlreadyExists() throws IOException {
		Files.createDirectories(Paths.get("./test/output/test/source"));
		Files.createFile(Paths.get("./test/output/test/source/file.txt"));
		
		FileInputStream is = new FileInputStream(ARCHIVE_FILE_NAME);
		Decoder decoder = new Decoder(is);
		decoder.setParentPath(PARRENT_PATH);
		boolean result = decoder.unpack();
		if(System.getProperty("os.name").contains("Windows")) {
			assertEquals("File: test\\output\\test\\source\\file.txt already exists\n", decoder.getErrorMessage());
		}else {
			assertEquals("File: test/output/test/source/file.txt already exists\n", decoder.getErrorMessage());
		}
		assertEquals(false, result);
		
		is.close();
		cleanOutputFolder();
	}
	
	@Test
	public void errorOnCorruptedInputStream() throws IOException, ClassNotFoundException {
		PipedOutputStream pos = new PipedOutputStream();
		PipedInputStream pis = new PipedInputStream(pos);
		
		ObjectOutputStream out = new ObjectOutputStream(pos);
		ObjectInputStream in = new ObjectInputStream (new FileInputStream(ARCHIVE_FILE_NAME));

		out.writeFloat(1f);
		in.readObject();
		out.write(in.readAllBytes());
		
		out.close();
		in.close();
		
		Decoder decoder = new Decoder(pis);
		decoder.setParentPath(PARRENT_PATH);
		boolean result = decoder.unpack();
		assertEquals(false, result);
		assertEquals("Error occurred while input stream decoding: "
				+ "Corrupted input stream format: cannot read path header", decoder.getErrorMessage());
		
		pis.close();
	}
}
