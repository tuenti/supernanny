/**
 * Dependency definition for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny.strategy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;

import com.google.inject.Inject;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;

/**
 * Dependency handler hg for SuperNanny.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author David Santiago <dsantiago@tuenti.com> Adding ArtifactInfo
 */
public class HgStrategy extends DvcsStrategy {
	private static final String HG = "hg";
	@Inject
	private Util util;

	@Override
	public void init(File depFolder, String url) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" init ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append("");
		util.readProcess(stringBuilder.toString());
	}

	@Override
	public String checkout(File depFolder, String uri, String version, String changeset)
			throws IOException {

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" --repository ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append(" update ");
		stringBuilder.append(changeset);
		util.readProcess(stringBuilder.toString());

		return changeset;
	}

	@Override
	public String pull(File depFolder, String uri, String name, String version, String changeset)
			throws IOException {
		// pull
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" --repository ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append(" pull ");
		stringBuilder.append(uri);
		stringBuilder.append(" -r ");
		stringBuilder.append(changeset);
		util.readProcess(stringBuilder.toString());

		// add path
		try {
			// Create file
			File f = new File(depFolder.getAbsolutePath() + File.separator + ".hg", "hgrc");
			FileWriter fstream = new FileWriter(f);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(MessageFormat.format("[paths]\ndefault = {0}\n", uri));
			out.close();
		} catch (Exception e) {
			throw new SuperNannyError(MessageFormat.format(
					"Cannot write to .hg/hgrc of {0}... Exiting...", depFolder));
		}

		return version;
	}

	@Override
	public void makeTag(File depFolder, String uri, String tagName)
			throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" tag ");
		stringBuilder.append(tagName);
		util.readProcess(stringBuilder.toString());
		stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		// hg is stupid must push next revision from tagging
		stringBuilder.append(" push -r .");
		util.readProcess(stringBuilder.toString());
	}

	@Override
	public String[] getTags(String uri, String name) throws IOException {
		if (!uri.startsWith("http")) {
			throw new RuntimeException("Getting tags from repos without web interface is not supported: " + uri);
		}

		URL tagsUrl;
		try {
			// TODO MUST MUST MUST MUST MUST MUST MUST FIX NO AUTH ON
			// code.tuenti.int
			tagsUrl = new URL(MessageFormat.format("{0}/tags?style=raw", uri));
			BufferedReader in = new BufferedReader(new InputStreamReader(tagsUrl.openStream()));

			String inputLine;
			ArrayList<String> tags = new ArrayList<String>();
			while ((inputLine = in.readLine()) != null) {
				String[] parts = inputLine.split("\\s");

				// filter mercurial's imaginary tag tip
				if (!parts[0].equals("tip")) {
					tags.add(parts[0]);
				}
			}
			in.close();
			String[] retval = new String[tags.size()];
			tags.toArray(retval);
			return retval;
		} catch (MalformedURLException e) {
			throw new SuperNannyError(e);
		}
	}

	@Override
	protected void cleanup(File depFolder) {
		try {
			util.deleteDir(new File(depFolder, ".hg"));
		} catch (IOException e) {
			System.err.println(e);
		}
		new File(depFolder, ".hgtags").delete();
		new File(depFolder, ".hgignore").delete();
	}

	@Override
	public String resolveReference(String uri, String version) throws IOException {
		return util.readProcess("hg id " + uri + " -r " + version).split("\\s")[0];
	}

	@Override
	protected String getRepoChangeset(File repo) throws SuperNannyError, IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" --repository ");
		stringBuilder.append(repo.toString());
		stringBuilder.append(" id");
		return util.readProcess(stringBuilder.toString()).split("\\s")[0];
	}
}
