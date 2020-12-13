package com.olegermolaev84.archive.core;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.stream.Stream;

/**
 * Compresses and packs denoted files and directories into the denoted OutputStream
 *
 */
public class Coder extends Codec {
	/** Array of files and directories' names to the archived. */
	private final String[] pathNames;
	
	/** Output stream the archived data to be written to */
	private final ObjectOutputStream outputStream;
	
	/** Compression level. Value from 0 to 9 */
	private int compressionLevel = 9;
	
	/**
	 * Constructor.
	 * @param pathNames Array of files and directories' names to the archived.
	 * @param os Output stream the archived data to be written to
	 * @throws IOException will be thrown in case of IO errors
	 */
	public Coder(String[] pathNames, OutputStream os) throws IOException {
		this.pathNames = pathNames;
		this.outputStream = new ObjectOutputStream(new BufferedOutputStream(os));
	}
	
	/**
	 * Sets the compression level. Value is to be in range 0-9.
	 * @param compressionLevel Compression level. Value from 0 to 9
	 * @throws IllegalArgumentException will be thrown is the denoted compression level less then 0 or more then 9.
	 */
	public void setCompressionLevel(int compressionLevel) throws IllegalArgumentException{
		if(compressionLevel > 9 || compressionLevel < 0) {
			throw new IllegalArgumentException("Compression level is out of range (0-9). Geven value: " + compressionLevel);
		}
	}
	
	/**
	 * Checks each file from the denoted directories and files. Each file should exist, have read permissions 
	 * and the size of file should not exceed 2Gb
	 * @return <code>true</code> if all tests are passed, otherwise <code>false</code>
	 */
	private boolean checkFiles() {
		StringBuffer errors = new StringBuffer();
		
		Arrays.asList(pathNames).stream()
		.forEach((pathName)->{
			try(Stream<Path> paths = Files.walk(Path.of(pathName))){
					paths.parallel().forEach((path)->{
					if(!Files.isReadable(path)) {
						errors.append("File: " + path + " does not have read permissions" + "\n");
					} 
					else {
						try {
							if(Files.size(path)>Integer.MAX_VALUE) {
								errors.append("Size of file: + " + path + " exeeds " +  Integer.MAX_VALUE + " bytes\n");
							}
						} catch (IOException e) {
							errors.append("IO error occurred while files checking: " + e.getMessage() + "\n");
						}
					}
				
					numberOfPaths++;
				});
			} catch (IOException e) {
				errors.append("File: " + pathName + " does not exist" + "\n");
			}
		});
		
		errorMessage = errors.toString();
		return errorMessage.length() == 0;
	}
	
	/**
	 * Packs files into the archive. 
	 * @return <code>true</code> if archiving is performed successfully, otherwise <code>false</code>
	 * @throws IOException will be thrown in case of IO errors
	 */
	public boolean pack() throws IOException {
		if(!checkFiles()) {
			return shutdown(false);
		}
		
		StringBuffer errors = new StringBuffer();
		
		ExecutorCompletionService<SingleFileCoder> service 
			= new ExecutorCompletionService<SingleFileCoder>(executor);
		
		Arrays.asList(pathNames).stream()
		.forEach((pathName)->{
			try(Stream<Path> paths = Files.walk(Path.of(pathName))){
					paths.forEach((path)->{
						submitTask(path, service, errors);
				});
			} catch (IOException e) {
				errors.append("Error occured while files submitting: " + e.getMessage() + "\n");
			}
		});
		
		if(errors.length() > 0) {
			errorMessage = errors.toString();
			return shutdown(false);
		}
		
		//
		// Collect results into the output stream
		// 
		for(int i = 0; i < numberOfPaths; i++) {
			try {
				writeCompressedData(service.take().get());
			} catch (IOException | InterruptedException | ExecutionException e1) {
				errors.append("Error occurred while files compressing: " + e1.getMessage() + "\n");
			}
		}
		
		if(errors.length() > 0) {
			errorMessage = errors.toString();
			return shutdown(false);
		}
		
		return shutdown(true);
	}
	
	@Override
	protected boolean shutdown(boolean result) throws IOException {
		executor.shutdown();
		outputStream.close();
		return result;
	}
	
	/**
	 * Creates new Callable task for the given path. The task is responsible for packing
	 *  the denoted file. Then the task is submitted into the ExecutorCompletionService
	 * @param path file or directory to be packed
	 * @param service ExecutorCompletionService
	 * @param errors StringBuffer to collect error which can occur while the file packing.
	 */
	private void submitTask(Path path, 
			ExecutorCompletionService<SingleFileCoder> service, StringBuffer errors) {
		Callable<SingleFileCoder> task = ()->{
			SingleFileCoder coder = new SingleFileCoder(path, compressionLevel);
			try {
				coder.packFile();
			}catch (IOException e) {
				errors.append("Error occurred while packing of file: " + path + ", error: " + e + "\n");
			}
			return coder;
		};
		log.info("Submitting task for path: " + path);
		service.submit(task);	
	}
	
	/**
	 * Writes PathHeader and compressed data of the file  into the output stream. 
	 * The method is called for each Callable task when it is completed.
	 * @param coder		SingleFileCoder which already has compressed file's data
	 * @throws IOException will be thrown if some IO error occurred while the data is being written to the output stream
	 */
	private void writeCompressedData(SingleFileCoder coder) throws IOException {
		log.info("Writing data: " + coder);
		PathHeader header = coder.getPathHeader();
		
		outputStream.writeObject(header);
		outputStream.write(coder.getCompressedData(), 0, header.getSizeOfData());
	}
}
