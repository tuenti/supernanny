/**
 * Dependency definition for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.strategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.strategy.common.ArchiveStrategy;

/**
 * Dependency handler tar.gz for SuperNanny.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class TarGzStrategy extends ArchiveStrategy {
	@Inject
	Logger l;
	@Inject
	Util util;

	private File tmpFile = null;

	@Override
	public void init(File depFolder, String url) {
		depFolder.mkdirs();
	}

	@Override
	public String download(File depFolder, String uri, String version)
			throws IOException {
		tmpFile = util.getTmpFile();

		if (version.contains("*")) {
			version = matchVersion(depFolder, uri, version);
		}

		if (uri.startsWith("ssh")) {
			extractSsh(depFolder, uri, version);
		} else if (uri.startsWith("http")) {
			extractUrl(depFolder, uri, version);
		} else {
			throw new Error(
					"Unknown protocol! The URI must either start with http:// for http webdav or ssh:// for secure copy.");
		}

		return version;
	}

	/**
	 * Pull the file with scp.
	 * 
	 * @param depFolder
	 *            name of the dependency.
	 * @param uri
	 *            ssh path.
	 * @param version
	 *            to pull in.
	 */
	private void extractSsh(File depFolder, String uri, String version) {
		try {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("scp ");
			stringBuilder.append(uri.replace("ssh://", ""));
			stringBuilder.append(depFolder.getName());
			stringBuilder.append(Util.ARCHIVE_VERSION_DELIMITER);
			stringBuilder.append(version);
			stringBuilder.append(getArchiveExtension());
			stringBuilder.append(" ");
			stringBuilder.append(tmpFile.getCanonicalPath().toString());
			readProcess(stringBuilder.toString());
			l.fine("File downloaded to " + tmpFile.toString());
		} catch (SuperNannyError e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Pull the file from an URL
	 * 
	 * @param depFolder
	 *            name of the dependency.
	 * @param uri
	 *            URL path.
	 * @param version
	 *            to pull in.
	 */
	private void extractUrl(File depFolder, String uri, String version) {
		URL depUrl = null;
		if (!uri.endsWith("/")) {
			uri = uri + "/";
		}
		try {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(uri);
			stringBuilder.append(depFolder.getName());
			stringBuilder.append(Util.ARCHIVE_VERSION_DELIMITER);
			stringBuilder.append(version);
			stringBuilder.append(getArchiveExtension());
			depUrl = new URL(stringBuilder.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		// fetch the file
		ReadableByteChannel rbc;
		try {
			rbc = Channels.newChannel(depUrl.openStream());
			FileOutputStream fos = new FileOutputStream(tmpFile);
			// if this is not enough, we have bigger problems :)
			fos.getChannel().transferFrom(rbc, 0, 1 << 27);
			fos.flush();
			fos.close();
			l.fine("File downloaded to " + tmpFile.toString());
		} catch (IOException e) {
			throw new SuperNannyError(e);
		}
	}

	@Override
	public void decompress(File depFolder, String uri, String version)
			throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getExtractCmd());
		stringBuilder.append(tmpFile.toString());
		stringBuilder.append(" -C ");
		stringBuilder.append(depFolder.getAbsolutePath());
		// untar the file
		readProcess(stringBuilder.toString());
	}

	@Override
	protected String getArchiveExtension() {
		return ".tar.gz";
	}

	@Override
	public void publish(String name, File depFolder, String uri, String version)
			throws IOException {

		if (uri.startsWith("ssh")) {
			publishSsh(name, depFolder, uri, version);
		} else if (uri.startsWith("http")) {
			publishUrl(name, depFolder, uri, version);
		} else {
			throw new Error(
					"Unknown protocol! The URI must either start with http:// for http webdav or ssh:// for secure copy.");
		}

	}

	private void publishUrl(String name, File depFolder, String uri,
			String version) {
		String username = null;
		String password = null;
		try {
			username = util.readInput("[username:]");
			password = util.readPassword();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		String destinationName = MessageFormat.format("{0}{1}{2}{3}", name,
				Util.ARCHIVE_VERSION_DELIMITER, version, getArchiveExtension());

		Sardine sardine = SardineFactory.begin(username, password);
		File tmpFile = util.getTmpFile();

		try {
			compress(depFolder, tmpFile);
			InputStream fis = new FileInputStream(tmpFile);
			sardine.put(MessageFormat.format("{0}{1}", uri, destinationName),
					fis);

			l.info(MessageFormat.format("Package published to {0}{1}{2}{3}{4}",
					uri, name, Util.ARCHIVE_VERSION_DELIMITER, version,
					getArchiveExtension()));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SuperNannyError e) {
			e.printStackTrace();
		}
	}

	private void publishSsh(String name, File depFolder, String uri,
			String version) throws IOException, SuperNannyError {
		File tmpFile = util.getTmpFile();
		uri = uri.replace("ssh://", "");

		compress(depFolder, tmpFile);

		String destination = MessageFormat.format("{0}{1}{2}{3}{4}", uri, name,
				Util.ARCHIVE_VERSION_DELIMITER, version, getArchiveExtension());

		// upload the file
		StringBuilder stringBuilder;
		try {
			stringBuilder = new StringBuilder();
			stringBuilder.append("scp ");
			stringBuilder.append(tmpFile.getCanonicalPath().toString());
			stringBuilder.append(" ");
			stringBuilder.append(destination);
			readProcess(stringBuilder.toString());
			l.info("Package published to " + destination);
		} catch (SuperNannyError e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void compress(File depFolder, File tmpFile) throws IOException,
			SuperNannyError {
		// compress the folder
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getArchiveCmd());
		stringBuilder.append(tmpFile.getCanonicalPath().toString());
		stringBuilder.append(" -C ");
		stringBuilder.append(depFolder.getCanonicalPath().toString());
		stringBuilder.append(" .");
		readProcess(stringBuilder.toString());
	}

	@Override
	public String matchVersion(File depFolder, String uri, String versionPrefix) {
		if (uri.startsWith("ssh")) {
			return sshVersion(depFolder, uri, versionPrefix);
		} else if (uri.startsWith("http")) {
			return webDavVersion(depFolder, uri, versionPrefix);
		} else {
			throw new Error(
					"Unknown protocol! The URI must either start with http:// for http webdav or ssh:// for secure copy.");
		}
	}

	private String webDavVersion(final File depFolder, String uri,
			final String versionPrefix) {
		try {
			return getLatestVersion(getWebDavAvailabeVersion(depFolder, uri,
					versionPrefix));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private String[] getWebDavAvailabeVersion(final File depFolder, String uri,
			final String versionPrefix) {
		Sardine sardine = SardineFactory.begin();

		List<String> versions = new LinkedList<String>();
		try {
			// try cache
			List<DavResource> resources = util.getDavCahce(uri);

			if (resources == null) {
				resources = sardine.list(uri);
				util.setDavCache(uri, resources);
			}

			String ext = getArchiveExtension();
			for (DavResource file : resources) {
				if (!file.isDirectory()) {
					String version = file.getName();
					String ver = version.substring(
							version.lastIndexOf('-') + 1, version.length()
									- ext.length());
					if (versionPrefix.equals("*")) {
						if (version.startsWith(depFolder.getName())) {
							versions.add(ver);
						}
					} else if (version.startsWith(depFolder.getName()
							+ Util.ARCHIVE_VERSION_DELIMITER
							+ versionPrefix.replace("*", ""))) {
						versions.add(ver);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			handleVersionParseError(depFolder, versionPrefix);
		} catch (StringIndexOutOfBoundsException e) {
			// ignore -- webdav contains file in wrong format, skip it
		}

		String[] aVersions = new String[versions.size()];
		versions.toArray(aVersions);
		return aVersions;
	}

	private String sshVersion(File depFolder, String uri, String versionPrefix) {
		try {
			return getLatestVersion(getSshAvailableVersions(depFolder, uri,
					versionPrefix));
		} catch (IOException e) {
			e.printStackTrace();
			handleVersionParseError(depFolder, versionPrefix);
		}
		return versionPrefix;
	}

	public String[] getSshAvailableVersions(File depFolder, String uri,
			String versionPrefix) {
		uri = uri.replace("ssh://", "");
		String host = uri.substring(0, uri.indexOf(":"));
		String folder = uri.substring(uri.indexOf(":") + 1);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("ssh ");
		stringBuilder.append(host);
		stringBuilder.append(" find ");
		stringBuilder.append(folder);
		stringBuilder.append(" -name \"");
		stringBuilder.append(depFolder.getName());
		stringBuilder.append(Util.ARCHIVE_VERSION_DELIMITER);
		stringBuilder.append(versionPrefix);
		String ext = getArchiveExtension();
		stringBuilder.append(ext);
		stringBuilder.append("\"");
		String[] versions = null;

		String[] availableFiles;
		try {
			availableFiles = readProcess(stringBuilder.toString()).split("\\s");

			versions = new String[availableFiles.length];
			int i = 0;
			for (String version : availableFiles) {
				String ver = version.substring(version.lastIndexOf('-') + 1,
						version.length() - ext.length());
				versions[i++] = ver;
			}
		} catch (SuperNannyError e) {
			handleVersionParseError(depFolder, versionPrefix);
		} catch (IOException e) {
			handleVersionParseError(depFolder, versionPrefix);
		}

		return versions;
	}

	private void handleVersionParseError(File depFolder, String versionPrefix) {
		l.warning("No versions matched for " + versionPrefix + " for project "
				+ depFolder);
		System.exit(0);
	}

	@Override
	protected String getArchiveCmd() {
		return "tar --exclude=.hg* --exclude=.git* -czpf ";
	}

	@Override
	protected String getExtractCmd() {
		return "tar xvpf ";
	}

	@Override
	public String getLatestVersion(String name, String uri) {
		String[] availableVersions;
		if (uri.startsWith("ssh")) {
			availableVersions = getSshAvailableVersions(new File(name), uri,
					"*");
		} else if (uri.startsWith("http")) {
			availableVersions = getWebDavAvailabeVersion(new File(name), uri,
					"*");
		} else {
			throw new Error(
					"Unknown protocol! The URI must either start with http:// for http webdav or ssh:// for secure copy.");
		}
		try {
			return getLatestVersion(availableVersions);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}