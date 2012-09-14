/**
 * Dependency definition for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.strategy.common;

import java.io.File;
import java.io.IOException;

/**
 * Dependency handler interface for SuperNanny.
 * 
 * All dependencies call the handler to fetch them.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public abstract class ArchiveStrategy extends DepStrategy {
	@Override
	public String fetch(File depFolder, String uri, String version)
			throws IOException {

		// download the dependency to a temp file
		version = download(depFolder, uri, version);

		// uncompress the archive to the proper folder
		decompress(depFolder, uri, version);

		return version;
	}

	/**
	 * Get the dependency locally.
	 * 
	 * @param depFolder
	 *            the folder the dependency will occupy.
	 * @param uri
	 *            URI of the dependency.
	 * @param version
	 *            of the dependency to fetch.
	 * @return version downloaded.
	 * @throws IOException
	 */
	public abstract String download(File depFolder, String uri, String version)
			throws IOException;

	/**
	 * Do an actual checkout after fetching the dependency.
	 * 
	 * This action can range from git checkout, unzip, untar or copying a
	 * folder.
	 * 
	 * @param depFolder
	 *            the folder the dependency will occupy.
	 * @param uri
	 *            URI of the dependency.
	 * @param version
	 *            of the dependency to fetch.
	 * @throws IOException
	 */
	public abstract void decompress(File depFolder, String uri, String version)
			throws IOException;

	/**
	 * Archive extension (e.g. .tar.gz, .zip, .rar, .tar.bz2. ...).
	 * 
	 * @return the extension
	 */
	protected abstract String getArchiveExtension();

	/**
	 * Archive command for the archive type.
	 * 
	 * @return the archive command.
	 */
	protected abstract String getArchiveCmd();

	/**
	 * Extract command for the archive type.
	 * 
	 * @return the extract command.
	 */
	protected abstract String getExtractCmd();
}