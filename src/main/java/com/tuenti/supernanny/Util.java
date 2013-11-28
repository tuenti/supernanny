/**
 * Utilities.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.artifacts.Export;
import com.tuenti.supernanny.repo.artifacts.Requirement;

/**
 * Utility interface for SuperNanny.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public interface Util {
	public static final String ARCHIVE_VERSION_DELIMITER = "-";
	public static final String ARCHIVE_SUFFIX_DELIMITER = "_";
	public static final String SUPERNANNY_VERSION_FILE = ".supernanny";
	public static final String DEP_FILE = ".DEP";
	public static final String EXPORT_FILE = ".EXPORT";
	public static final String DEP_FOLDER = "lib";
	public static final String COMMENT_START_CHAR = "#";
	public static final Integer EXECUTOR_POOL_SIZE = 10;
	public static final String VERSION = "Supernanny 2.2";
	public static final String PROPERTIES_FILE = "supernanny.properties";
	public static final String LIB_DELIMITER = "_";

	/**
	 * Parse the exports file.
	 * 
	 * @param exportsFile
	 *            file containing dep definition.
	 * @return collection of export deps parsed.
	 * @throws IOException
	 */
	public abstract List<Export> parseExportsFile(File exportsFile) throws IOException;

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
	public abstract File getDepsFolder();

	/**
	 * Delete a directory recursively
	 * 
	 * @param dir
	 * @throws IOException
	 */
	public abstract void deleteDir(File dir) throws IOException;

	/**
	 * Stamp the project by writing a SuperNanny version file to it.
	 * 
	 * Format of the stamp is:
	 * 
	 * timestamp repo_type uri version
	 * 
	 * @param name
	 *            the name of the dependency
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
	public abstract void stampProject(String name, File depFolder, String uri, String version,
			RepositoryType type) throws IOException;

	/**
	 * Read a password from the console.
	 * 
	 * @return the password read.
	 * @throws IOException
	 *             on terminal error.
	 */
	public String readPassword() throws IOException;

	/**
	 * Set the project's root folder.
	 * 
	 * @param projectPath
	 *            abstract root folder.
	 */
	public abstract void setRoot(File projectPath);

	/**
	 * Get the project's root folder.
	 * 
	 * @return the project's root file.
	 */
	public abstract File getRoot();

	/**
	 * Get the username/password for services form properties file.
	 * 
	 * @return array of username and password.
	 */
	public String[] getCredentialsFromProperties() throws IOException;

	/**
	 * Ask the user for confirmation
	 * 
	 * @param string
	 * @return true if the user wants to proceed
	 * @throws IOException
	 */
	public abstract boolean confirmYN(String string) throws IOException;

	/**
	 * Check if the given file appears to be a symlink
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public abstract boolean isSymlink(File f) throws IOException;

	/**
	 * Get information on the currently checked out library
	 * 
	 * @param folder
	 * @return Requirement of the current version
	 * @throws IOException
	 */
	public abstract Requirement getProjectInfo(File folder) throws IOException;

	/**
	 * Execute a process and read its response
	 * 
	 * @param command
	 *            Command to run
	 * @return Command's stdout
	 * @throws SuperNannyError
	 * @throws IOException
	 */
	public String readProcess(String command) throws SuperNannyError, IOException;

	/**
	 * Read a file returning all lines read
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	Iterable<String> lineByLine(File f) throws IOException;

	/**
	 * Print a matrix of strings choosing an appropriate width for each column
	 * 
	 * @param rows
	 *            Rows to print
	 * @param prefix
	 *            PRefix to prepend to all rows (useful for indenting)
	 * @param colSep
	 *            String to include between columns
	 */
	public void printColumns(List<String[]> rows, String prefix, String colSep);

	/**
	 * Print columns sorting them by the given column
	 * 
	 * @param rows
	 * @param prefix
	 * @param colSep
	 * @param sortCol
	 * @param ascending
	 */
	void printColumns(List<String[]> rows, String prefix, String colSep, int sortCol,
			boolean ascending);

	/**
	 * Sort rows by the given column
	 * 
	 * @param rows
	 * @param sortCol
	 * @param ascending
	 */
	void sortRowsByColumn(List<String[]> rows, int sortCol, boolean ascending);

	/**
	 * Run the given process and return status
	 * 
	 * @param command
	 * @return status
	 * @throws IOException
	 */
	int execProcess(String command) throws IOException;

}
