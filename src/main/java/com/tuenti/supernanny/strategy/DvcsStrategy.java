/**
 * Dependency definition for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny.strategy;

import java.io.File;
import java.io.IOException;

import com.google.inject.Inject;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;

/**
 * Dependency handler interface for SuperNanny.
 * 
 * All dependencies call the handler to fetch them.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public abstract class DvcsStrategy {
	@Inject
	private Util util;

	protected abstract String getRepoChangeset(File repo) throws SuperNannyError, IOException;

	public String checkoutVersion(String uri, String name, String version, String changeset,
			File repo) throws IOException {
		String currentVersion = "";
		try {
			currentVersion = getRepoChangeset(repo);
		} catch (Throwable t) {
			// just go ahead and checkout
		}
		if (!currentVersion.equals(changeset)) {
			// fetch the dependency
			pull(repo, uri, name, version, changeset);

			// check out the required version
			currentVersion = checkout(repo, uri, version, changeset);
		}
		return currentVersion;
	}

	public String fetch(String uri, String name, String version, String changeset, String path,
			File destination, File repo) throws IOException {
		if (destination.exists()) {
			util.deleteDir(destination);
		}

		version = checkoutVersion(uri, name, version, changeset, repo);

		String source = repo.getAbsolutePath();
		if (path != null && !"".equals(path)) {
			source += File.separator + path;
			if (!new File(source).exists()) {
				throw new SuperNannyError("The specified subpath \"" + path + "\" doesn't exist!");
			}
		}

		// resolve symlinks pointing outside of the copied directory
		util.readProcess("rsync --archive --delete --copy-unsafe-links " + source +"/ " + destination);

		cleanup(destination);
		return version;
	}

	/**
	 * Cleanup all CVS related information. Should leave a clean export.
	 * 
	 * @param depFolder
	 */
	protected abstract void cleanup(File depFolder);

	/**
	 * Get the dependency locally.
	 * 
	 * @param name
	 *            the name of the dependency
	 * @param depFolder
	 *            the folder the dependency will occupy.
	 * @param uri
	 *            URI of the dependency.
	 * @param version
	 *            of the dependency to fetch.
	 * @param changeset
	 * @return actual version pulled
	 * @throws IOException
	 */
	protected abstract String pull(File depFolder, String uri, String name, String version,
			String changeset) throws IOException;

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
	 * @param changeset
	 * @return revision checked out.
	 * @throws IOException
	 */
	protected abstract String checkout(File depFolder, String uri, String version, String changeset)
			throws IOException;

	/**
	 * Initialize the dependency.
	 * 
	 * Usually create an empty repo or a folder.
	 * 
	 * @param depFolder
	 *            the folder the dependency will occupy.
	 * @param uri
	 *            URI of the dependency.
	 * @throws IOException
	 */
	public abstract void init(File depFolder, String uri) throws IOException;

	public abstract String resolveReference(String uri, String version) throws IOException;

	public String fetch(String uri, String name, String version, String path, File checkoutFolder)
			throws IOException {
		throw new RuntimeException("Should not be called");
	}

	public abstract String[] getTags(String uri, String name) throws SuperNannyError, IOException;

	public abstract void makeTag(File depFolder, String uri, String tagName) throws IOException;
}
