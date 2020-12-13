package com.olegermolaev84.archive.core;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements common logic for both Coder and Decoder classes
 */
public abstract class Codec {
	protected Logger log = Logger.getGlobal();
	
	/** Threads executor. number of threads = number of processors */
	protected ExecutorService executor; 
	
	/** Contains error description in case of any failures of coding or decoding */
	protected String errorMessage = "";
	
	/** Total number of files and directories to be archived/dearchived. */
	protected int numberOfPaths = 0;
	
	public Codec() {
		executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		Logger.getGlobal().setLevel(Level.WARNING);
	}
	
	/**
	 * Sets logger. By default the Logger.getGlobal() is used
	 * @param log Logger to be set
	 */
	public void setLogger(Logger log) {
		Objects.nonNull(log);
		this.log = log;
	}
	
	/**
	 * Returns error description in case of any failures of coding or decoding
	 * @return error description
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * Shutdowns executor and closes streams
	 * @param result the result to be returned by this method
	 * @return result 
	 * @throws IOException will be thrown in case of IO exceptions
	 */
	protected abstract boolean shutdown(boolean result) throws IOException;
}
