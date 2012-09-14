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
public abstract class DvcsStrategy extends DepStrategy {
	@Override
	public String fetch(File depFolder, String uri, String version)
			throws IOException {

		// fetch the dependency
		version = pull(depFolder, uri, version);

		// check out the required version
		checkout(depFolder, uri, version);

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
	 * @return version pulled.
	 * @throws IOException
	 */
	public abstract String pull(File depFolder, String uri, String version)
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
	public abstract void checkout(File depFolder, String uri, String version)
			throws IOException;
}