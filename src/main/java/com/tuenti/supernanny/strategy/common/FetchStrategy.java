/**
 * Fetchable dependencies contract.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.strategy.common;

import java.io.File;
import java.io.IOException;

/**
 * Interface for fetching dependencies.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public interface FetchStrategy {
	/**
	 * Get the dependency locally.
	 * 
	 * @param depFolder
	 *            the folder the dependency will occupy.
	 * @param uri
	 *            URI of the dependency.
	 * @param version
	 *            of the dependency to fetch.
	 * @return version fetched
	 * @throws IOException
	 */
	String fetch(File depFolder, String uri, String version) throws IOException;
}