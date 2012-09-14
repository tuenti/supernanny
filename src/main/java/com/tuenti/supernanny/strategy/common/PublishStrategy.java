/**
 * Publishable dependencies contract.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.strategy.common;

import java.io.File;
import java.io.IOException;

/**
 * Interface for publishing dependencies.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public interface PublishStrategy {
	/**
	 * Publish the dependency.
	 * 
	 * @param name
	 *            of the artifact.
	 * @param depFolder
	 *            of the target's root.
	 * @param uri
	 *            URI of the dependency.
	 * @param version
	 *            of the dependency to fetch.
	 * @throws IOException
	 */
	void publish(String name, File depFolder, String uri, String version)
			throws IOException;
}