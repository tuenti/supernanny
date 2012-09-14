/**
 * Utilities.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.googlecode.sardine.DavResource;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.dependencies.Dependency;
import com.tuenti.supernanny.dependencies.Dependency.DepType;
import com.tuenti.supernanny.dependencies.NoOpDependency;

/**
 * Utility interface for SuperNanny.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public interface Util {
	public static final String ARCHIVE_VERSION_DELIMITER = "-";
	public static final String SUPERNANNY_VERSION_FILE = ".supernanny";
	public static final String DEP_FILE = ".DEP";
	public static final String EXPORT_FILE = ".EXPORT";
	public static final String DEP_FOLDER = "lib";
	public static final String COMMENT_START_CHAR = "#";
	public static final Integer EXECUTOR_POOL_SIZE = 10;
	public static final String VERSION = "Supernanny 1.10";

	/**
	 * Parse the dependencies file.
	 * 
	 * @param depsFile
	 *            file containing dep definition.
	 * @return collection of dependencies parsed.
	 */
	public abstract LinkedList<Dependency> parseDepsFile(File depsFile);
	
	/**
	 * Parse a list of dependency files.
	 * 
	 * @param depsFiles
	 *            list of files containing dep definition.
	 * @return collection of dependencies parsed.
	 */
	public abstract LinkedList<Dependency> parseMultipleDepFiles(Iterable<File> depsFile);

	/**
	 * Parse the exports file.
	 * 
	 * @param exportsFile
	 *            file containing dep definition.
	 * @return collection of export deps parsed.
	 */
	public abstract LinkedList<Dependency> parseExportsFile(File exportsFile);

	/**
	 * Read input from the terminal.
	 * 
	 * @param message
	 *            to display before input.
	 * @return the read line
	 * @throws IOException
	 *             if reading from terminal fails.
	 */
	public abstract String readInput(String message) throws IOException;

	/**
	 * Assigns a temporary file in the system's temp folder to use for a
	 * download.
	 * 
	 * @return a (hopefully) non-existing file.
	 */
	public abstract File getTmpFile();

	/**
	 * All dependencies will be fetched under this folder.
	 * 
	 * @return dependencies folder.
	 */
	public abstract String getDepsFolder();

	public abstract void deleteDir(File dir);

	/**
	 * Stamp the project by writing a SuperNanny version file to it.
	 * 
	 * Format of the stamp is:
	 * 
	 * timestamp repo_type uri version
	 * 
	 * @param depFolder
	 *            the folder the dependency will occupy.
	 * @param uri
	 *            URI of the dependency.
	 * @param version
	 *            of the dependency to fetch.
	 * @param type
	 *            of the dependency.
	 * @throws IOException
	 *             if file writing error happens.
	 */
	public abstract void stampProject(File depFolder, String uri,
			String version, DepType type) throws IOException;

	/**
	 * Get the project's dependency type.
	 * 
	 * @param depRoot
	 *            root of the project.
	 * @return type of the dependency.
	 * @throws IOException
	 *             if the project metadata cannot be read.
	 * @throws NoOpDependency if NOOP type is selected.
	 */
	public abstract DepType getProjectDepType(File depRoot) throws IOException, NoOpDependency;

	/**
	 * Get the project's dependency type.
	 * 
	 * @param depRoot
	 *            root of the project.
	 * @return type of the dependency.
	 * @throws IOException
	 *             if the project metadata cannot be read.
	 * @throws NoOpDependency if NOOP type is selected.
	 */
	public Dependency getProjectDependancy(File depRoot) throws IOException, NoOpDependency;

	/**
	 * Implode the array, gluing elements with <var>glueString</var>.
	 * 
	 * @param inputArray
	 *            array of strings.
	 * @param glueString
	 *            string to use as a delimiter.
	 * @return imploded string.
	 */
	public String implodeArray(String[] inputArray, String glueString);

	/**
	 * Returns a file combining the folder and the file name.
	 * 
	 * @param projectPath
	 *            folder path.
	 * @param depFile
	 *            abstract name of the file.
	 * @return requested file.
	 */
	public abstract File getFile(File projectPath, String depFile);

	/**
	 * Returns the dependency's version.
	 * 
	 * @param depRoot
	 *            root of the project.
	 * 
	 * @return version of the dependency.
	 * @throws IOException
	 *             if the project metadata cannot be read.
	 */
	public abstract String getProjectVersion(File depRoot) throws IOException;
	
	public abstract String getNextVersion(String format, String latest);

	/**
	 * Parse versions from cli.
	 * @param versions cli input.
	 * @returns map of dependency to version.
	 */
	public abstract Map<String, String> parseForcedVersions(String[] versions);
	
	/**
	 * Read a password from the console.
	 * @return the password read.
	 * @throws IOException on terminal error.
	 */
	public String readPassword() throws IOException;
	
	/**
	 * Read the file, line by line, ignoring comments.
	 * 
	 * @param f
	 *            file to read.
	 * @return iterator of lines
	 * @throws IOException
	 *             if something goes wrong while reading the file.
	 */
	public Iterable<String> lineByLine(File f) throws IOException;

	/**
	 * Set the project's root folder.
	 * 
	 * @param projectPath abstract root folder.
	 */
	public abstract void setRoot(File projectPath);
	
	/**
	 * Get the project's root folder.
	 * 
	 * @return the project's root file.
	 */
	public abstract File getRoot();

	/**
	 * Get deps form multiple files.
	 * 
	 * @param p CliParser with the input.
	 * @return list of dependencies
	 */
	LinkedList<Dependency> parseMultipleDepFiles(CliParser p);

	/**
	 * Get the cache from web dav.
	 * 
	 * @param uri http uri of the dav server.
	 * @return list of files on the server.
	 */
	public abstract List<DavResource> getDavCahce(String uri);

	/**
	 * Get the cache from web dav.
	 * 
	 * @return list of files on the server.
	 * @param uri http uri of the dav server.
	 * @param resources to persist.
	 */
	public abstract void setDavCache(String uri, List<DavResource> resources);
}
