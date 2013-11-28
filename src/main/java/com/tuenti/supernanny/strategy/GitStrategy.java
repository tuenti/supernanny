/**
 * Dependency definition for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author David Santiago <dsantiago@tuenti.com> Adding ArtifactInfo
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny.strategy;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import com.google.inject.Inject;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;

/**
 * Dependency handler git for SuperNanny.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class GitStrategy extends DvcsStrategy {
	private static final String GIT = "git";
	@Inject
	Util util;

	@Override
	public void init(File depFolder, String uri) throws SuperNannyError, IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" init ");
		stringBuilder.append(depFolder.toString());
		util.readProcess(stringBuilder.toString());
		stringBuilder = this.getGitInitCommandStringFor(depFolder);
		stringBuilder.append("remote add origin ");
		stringBuilder.append(uri);
		stringBuilder.append("");
		util.readProcess(stringBuilder.toString());
	}

	@Override
	public String checkout(File depFolder, String uri, String version, String changeset)
			throws SuperNannyError, IOException {

		StringBuilder stringBuilder = getGitInitCommandStringFor(depFolder);
		stringBuilder.append("--work-tree ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append(" checkout " + changeset);
		util.readProcess(stringBuilder.toString());

		return changeset;
	}

	@Override
	public String pull(File depFolder, String uri, String name, String version, String changeset)
			throws IOException {
		StringBuilder stringBuilder = getGitInitCommandStringFor(depFolder);
		stringBuilder.append("fetch origin ");
		stringBuilder.append(version);
		util.readProcess(stringBuilder.toString());

		return version;
	}

	@Override
	public void makeTag(File depFolder, String uri, String tagName) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" tag ");
		stringBuilder.append(tagName);
		util.readProcess(stringBuilder.toString());

		stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" push origin tag ");
		stringBuilder.append(tagName);
		util.readProcess(stringBuilder.toString());
	}

	@Override
	public String resolveReference(String uri, String ref) throws IOException {
		String[] split = util.readProcess("git ls-remote " + uri + " " + ref).split("\\s");
		if (split.length>0) {
			String changeset = split[0].trim();
			if (!"".equals(changeset)) {
				return changeset;
			}
		}
		return null;
	}

	@Override
	public String[] getTags(String uri, String name) throws SuperNannyError, IOException {
		String tagPrefix = name + Util.ARCHIVE_VERSION_DELIMITER;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" ls-remote --tags ");
		stringBuilder.append(uri);
		stringBuilder.append(" ");
		stringBuilder.append(tagPrefix + "*");

		// get tags in format - list of lines of format: SHA tagName
		String[] fullTags = util.readProcess(stringBuilder.toString()).split("\\n");
		String[] tags = new String[fullTags.length];

		// split tag names only
		int i = 0;
		for (String tag : fullTags) {
			String[] splitTags = tag.split("\\s");
			if (splitTags.length != 2) {
				throw new SuperNannyError(MessageFormat.format(
						"Tags not found in {1}. Make sure the correct tag exists.", uri));
			} else {
				tags[i++] = splitTags[1].replace("refs/tags/", "");
			}
		}

		return tags;
	}

	/**
	 * Builds the initial part of a git command invocation for the given folder.
	 * 
	 * @author David Santiago <dsantiago@tuenti.com>
	 * @param folder
	 *            The folder for the Git repo.
	 * @return StringBuilder Initial part of the git command invocation.
	 */
	private StringBuilder getGitInitCommandStringFor(File folder) {
		return this.getGitInitCommandStringFor(folder.toString());
	}

	/**
	 * Builds the initial part of a git command invocation for the given folder.
	 * 
	 * @author David Santiago <dsantiago@tuenti.com>
	 * @param folder
	 *            The folder for the Git repo.
	 * @return StringBuilder Initial part of the git command invocation.
	 */
	private StringBuilder getGitInitCommandStringFor(String folder) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" --git-dir ");
		stringBuilder.append(folder);
		stringBuilder.append("/.git ");

		return stringBuilder;
	}

	@Override
	protected void cleanup(File depFolder) {
		try {
			util.deleteDir(new File(depFolder, ".git"));
		} catch (IOException e) {
			System.err.println(e);
		}
		new File(depFolder, ".gitignore").delete();
	}

	@Override
	protected String getRepoChangeset(File repo) throws SuperNannyError, IOException {
		StringBuilder stringBuilder = getGitInitCommandStringFor(repo);
		stringBuilder.append(" rev-parse HEAD");
		return util.readProcess(stringBuilder.toString()).trim();
	}
}
