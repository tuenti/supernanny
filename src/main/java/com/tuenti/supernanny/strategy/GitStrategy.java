/**
 * Dependency definition for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.strategy;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.strategy.common.DvcsStrategy;

/**
 * Dependency handler git for SuperNanny.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class GitStrategy extends DvcsStrategy {
	private static final String GIT = "git";

	@Override
	public void init(File depFolder, String url) throws SuperNannyError,
			IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" init ");
		stringBuilder.append(depFolder.toString());
		readProcess(stringBuilder.toString());
		stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" --git-dir ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append("/.git remote add origin ");
		stringBuilder.append(url);
		stringBuilder.append("");
		readProcess(stringBuilder.toString());
	}

	@Override
	public void checkout(File depFolder, String uri, String version)
			throws SuperNannyError, IOException {

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" --git-dir ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append("/.git --work-tree ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append(" checkout FETCH_HEAD");
		readProcess(stringBuilder.toString());
	}

	@Override
	public String pull(File depFolder, String uri, String version)
			throws IOException {

		if (version.contains("*")) {
			version = matchVersion(depFolder, uri, version);
		}
		
		version.replace("refs/tags/", "");

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" --git-dir ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append("/.git fetch origin ");
		stringBuilder.append(version);
		readProcess(stringBuilder.toString());

		return version;
	}

	@Override
	public void publish(String name, File depFolder, String uri, String version)
			throws IOException {
		String gitFolder = ".";
		version = version.replace("refs/tags/", "");

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" --git-dir ");

		stringBuilder.append(gitFolder);
		stringBuilder.append("/.git tag ");
		stringBuilder.append(version);
		readProcess(stringBuilder.toString());
		stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" --git-dir ");
		stringBuilder.append(gitFolder);
		stringBuilder.append("/.git push origin tag ");
		stringBuilder.append(version);
		readProcess(stringBuilder.toString());
	}

	@Override
	public String matchVersion(File depFolder, String uri, String versionPrefix) {
		String latestVersion = null;
		try {
			latestVersion = getLatestVersion(getTags(uri, versionPrefix));
		} catch (SuperNannyError e) {
			l.warning("No versions matched for " + versionPrefix
					+ " for project " + depFolder);
			System.exit(0);
		} catch (IOException e) {
			l.warning("No versions matched for " + versionPrefix
					+ " for project " + depFolder);
			System.exit(0);
		}
		return latestVersion;
	}

	private String[] getTags(String uri, String versionPrefix)
			throws SuperNannyError, IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(GIT);
		stringBuilder.append(" ls-remote --tags ");
		stringBuilder.append(uri);
		stringBuilder.append(" ");
		stringBuilder.append(versionPrefix);

		// get tags in format - list of lines of format: SHA tagName
		String[] fullTags = readProcess(stringBuilder.toString()).split("\\n");
		String[] tags = new String[fullTags.length];

		// split tag names only
		int i = 0;

		for (String tag : fullTags) {
			String[] splitTags = tag.split("\\s");
			if (splitTags.length != 2) {
				System.err
						.println(MessageFormat
								.format("Tag matching {0} not found in {1}. Make sure the correc tag exists.",
										versionPrefix, uri));
				System.exit(1);
			} else {
				tags[i++] = splitTags[1];
			}
		}

		return tags;
	}

	@Override
	public String getLatestVersion(String name, String uri) {
		try {
			return "refs/tags/" + getLatestVersion(getTags(uri, ""));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SuperNannyError e) {
			e.printStackTrace();
		}
		return null;
	}
}
