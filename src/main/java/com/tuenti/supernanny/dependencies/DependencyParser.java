package com.tuenti.supernanny.dependencies;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.repo.artifacts.Requirement;

public interface DependencyParser {
	/**
	 * Get deps form multiple files.
	 * 
	 * @param p CliParser with the input.
	 * @return list of dependencies
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	List<Requirement> parseMultipleDepFiles(CliParser p) throws IOException, InvalidFormatException;

	/**
	 * Parse the dependencies file.
	 * 
	 * @param depsFile
	 *            file containing dep definition.
	 * @return collection of dependencies parsed.
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	public abstract List<Requirement> parseDepsFile(File depsFile) throws IOException, InvalidFormatException;

	/**
	 * Parse a list of dependency files.
	 * 
	 * @param depsFiles
	 *            list of files containing dep definition.
	 * @return collection of dependencies parsed.
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	public abstract List<Requirement> parseMultipleDepFiles(Iterable<File> depsFile) throws IOException, InvalidFormatException;

	/**
	 * Parse a dep file from a list of lines instead of a file
	 * @param lines
	 * @return
	 * @throws InvalidFormatException
	 */
	List<Requirement> parseDeps(Iterable<String> lines) throws InvalidFormatException;
}
