package com.olegermolaev84.archive.core;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

import com.olegermolaev84.archive.util.FileFormatException;


/**
 * Unpacks files from the denoted stream and saves them 
 * into the specified directory (by default the current directory)
 *
 */
public class Decoder extends Codec{
	/** Stream with packed files */
	private final ObjectInputStream inputStream;
	
	/** Path to the directory the unpacked files to be stored to */
	private Path parrentPath = Paths.get(".");
	
	/**
	 * Constructor 
	 * @param is Stream with packed files
	 * @throws IOException will be thrown in case of IO errors
	 */
	public Decoder(InputStream is) throws IOException {
		this.inputStream = new ObjectInputStream(new BufferedInputStream(is));
	}
	
	/**
	 * Sets path to the directory the unpacked files to be stored to
	 * @param parrentPath path to the directory the unpacked files to be stored to
	 * @throws IllegalArgumentException will be thrown in cases:
	 * <br>-Denoted path does not exist
	 * <br>-Denoted path is not directory
	 * <br>-Denoted path does not have write permissions
	 */
	public void setParentPath(Path parrentPath) throws IllegalArgumentException {
		Objects.nonNull(parrentPath);
		if(!Files.exists(parrentPath)) {
			throw new IllegalArgumentException("Path: " + parrentPath + " does not exist");
		}
		else if(!Files.isDirectory(parrentPath)){
			throw new IllegalArgumentException("Path: " + parrentPath + " is not directory");
		}
		else if(!Files.isWritable(parrentPath)) {
			throw new IllegalArgumentException("Path: " + parrentPath + " does not have write permissions");
		}
		this.parrentPath = parrentPath;
	}
	
	/**
	 * Unpacks files from the input stream
	 * @return <code>true</code> if unpacking is performed successfully, otherwise <code>false</code>
	 * @throws IOException will be thrown in case of IO errors
	 */
	public boolean unpack() throws IOException {
		StringBuffer errors = new StringBuffer();
		
		if(!Files.isWritable(parrentPath)) {
			errorMessage = "Path: " + parrentPath + " does not have write permissions";
			return shutdown(false);
		}
		
		ExecutorCompletionService<Void> service 
			= new ExecutorCompletionService<Void>(executor);
		
		try {
			SingleFileDecoder fileDecoder = readPackedData();
			while (fileDecoder != null) {
				submitTask(fileDecoder, service, errors);
				fileDecoder = readPackedData();
			}
		} catch (FileFormatException e) {
			errorMessage = "Error occurred while input stream decoding: " + e.getMessage();
			return shutdown(false);
		}
		
		// wait till all files decoded
		for(int i = 0; i < numberOfPaths; i++) {
			try {
				service.take().get();
			} catch (InterruptedException | ExecutionException e) {
				errors.append("Error occurred while files unpacking: " + e.getMessage() + "\n");
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
		inputStream.close();
		return result;
	}
	
	/**
	 * Creates new Callable task for the given SingleFileDecoder. The task is responsible for unpacking
	 * and saving the file (or directory) to the output folder. Then the task is submitted into the 
	 * ExecutorCompletionService
	 * @param fileDecoder initialized with compressed data SingleFileDecoder class object.
	 * @param service ExecutorCompletionService
	 * @param errors StringBuffer to collect error which can occur while the file unpacking.
	 */
	private void submitTask(SingleFileDecoder fileDecoder, 
			ExecutorCompletionService<Void> service, StringBuffer errors) {
		Callable<Void> task =()->{
			try {
				fileDecoder.unpackFile();
			}catch (FileAlreadyExistsException e) {
				errors.append("File: " + fileDecoder.getFileName() + " already exists\n");
			}catch (IOException e) {
				errors.append("Error occurred while unpacking of file: " + fileDecoder.getFileName() + ", error: " + e + "\n");
			}
			return null;
		};
		service.submit(task);
	}

	/**
	 * Reads next PathHeader and the compressed data.
	 * Creates and initializes SingleFileDecoder class object.
	 * @return SingleFileDecoder class object or <code>null</code> if EOF is reached
	 * @throws FileFormatException will be thrown if the input stream has corrupted format
	 * @throws IOException  will be thrown in case of IO errors
	 */
	private SingleFileDecoder readPackedData() throws FileFormatException, IOException {		
		PathHeader header = null;
		byte[] data;
		Path path;
		
		try {
			header = (PathHeader)inputStream.readObject();
		} catch (EOFException e) {
			return null;
		} catch (ClassNotFoundException | OptionalDataException e) {
			throw new FileFormatException("Corrupted input stream format: cannot read path header");
		} 
		
		try {
			path = Paths.get(header.getPathName());
			path = parrentPath.resolve(path);
		}
		catch(InvalidPathException e) {
			throw new FileFormatException("Corrupted file format: file name has unsupported characters");
		}
		
		try {
			data = inputStream.readNBytes(header.getSizeOfData());
		} catch (IOException e) {
			throw new FileFormatException("Corrupted file format: cannot read file data");
		}
		
		if(data.length < header.getSizeOfData()) {
			throw new FileFormatException("Corrupted file format: cannot read file data");
		}
		
		log.info("Data read from the file: fileFlag =" + header.isRegularFile() +
				", path="+path.toString() +
				", sizeOfData="+header.getSizeOfData());
		
		numberOfPaths++;
		return new SingleFileDecoder(header.isRegularFile(), data, path);
	}
}
