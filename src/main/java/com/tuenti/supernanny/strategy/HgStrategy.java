/**
 * Dependency definition for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
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

import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.strategy.common.DvcsStrategy;

/**
 * Dependency handler hg for SuperNanny.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class HgStrategy extends DvcsStrategy {
	private static final String HG = "hg";
	private String revId = null;

	@Override
	public void init(File depFolder, String url) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" init ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append("");
		readProcess(stringBuilder.toString());
	}

	@Override
	public void checkout(File depFolder, String uri, String version) throws IOException {

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" --repository ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append(" update ");
		stringBuilder.append(revId);
		readProcess(stringBuilder.toString());
	}

	@Override
	public String pull(File depFolder, String uri, String version) throws IOException {

		if (version.contains("*")) {
			version = matchVersion(depFolder, uri, version);
		}

		// pull
		revId = readProcess("hg id -i " + uri + " -r " + version);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" --repository ");
		stringBuilder.append(depFolder.toString());
		stringBuilder.append(" pull ");
		stringBuilder.append(uri);
		stringBuilder.append(" -r ");
		stringBuilder.append(revId);
		readProcess(stringBuilder.toString());

		// add path
		try {
			// Create file
			File f = new File(depFolder.getAbsolutePath() + File.separator + ".hg", "hgrc");
			System.out.println("create " + f.getAbsolutePath() + " " + f.createNewFile());
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
	public void publish(String name, File depFolder, String uri, String version) throws IOException {
		String hgFolder = ".";
		String tagName = version;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" --repository ");
		stringBuilder.append(hgFolder);
		stringBuilder.append(" tag ");
		stringBuilder.append(tagName);
		readProcess(stringBuilder.toString());
		stringBuilder = new StringBuilder();
		stringBuilder.append(HG);
		stringBuilder.append(" --repository ");
		stringBuilder.append(hgFolder);
		// hg is stupid must push next revision from tagging
		stringBuilder.append(" push -r .");
		readProcess(stringBuilder.toString());
	}

	@Override
	public String matchVersion(File depFolder, String uri, String versionPrefix) {
		String latestVersion = null;
		try {
			latestVersion = getLatestVersion(getTags(uri, versionPrefix));
		} catch (SuperNannyError e) {
			l.warning("No versions matched for " + versionPrefix + " for project " + depFolder);
			System.exit(0);
		} catch (IOException e) {
			l.warning("No versions matched for " + versionPrefix + " for project " + depFolder);
			System.exit(0);
		}
		return latestVersion;
	}

	private String[] getTags(String uri, String versionPrefix) {
		if (uri.startsWith("http")) {
			return getScmSiteTags(uri, versionPrefix);
		}
		uri = uri.replace("hg://", "").replace("ssh://", "");
		String host = uri.substring(0, uri.indexOf("/"));
		String folder = uri.substring(uri.indexOf("/") + 1);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("ssh ");
		stringBuilder.append(host);
		stringBuilder.append(" " + HG + " --cwd ");
		stringBuilder.append(folder);
		stringBuilder.append(" cat .hgtags -r tip | cut -d' ' -f2");

		// check for grep
		if (versionPrefix.length() > 0) {
			stringBuilder.append(" | grep " + versionPrefix);
		}

		try {
			return readProcess(stringBuilder.toString()).split("\\s");
		} catch (SuperNannyError e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}

		return null;
	}

	private String[] getScmSiteTags(String uri, String versionPrefix) {
		URL tagsUrl;
		try {
			tagsUrl = new URL(MessageFormat.format("{0}/tags?style=raw", uri));
			BufferedReader in = new BufferedReader(new InputStreamReader(tagsUrl.openStream()));

			String inputLine;
			ArrayList<String> tags = new ArrayList<String>();
			while ((inputLine = in.readLine()) != null) {
				String[] parts = inputLine.split("\\s");

				// filter mercurial's imaginary tag tip
				if (!parts[0].equals("tip")
				    && (versionPrefix.length() == 0 || parts[0].startsWith(versionPrefix.replace("*", "")))) {
					tags.add(parts[0]);
				}
			}
			in.close();
			String[] retval = new String[tags.size()];
			tags.toArray(retval);
			return retval;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return new String[] {};
	}

	@Override
	public String getLatestVersion(String name, String uri) {
		try {
			return getLatestVersion(getTags(uri, ""));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
