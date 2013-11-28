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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.MessageFormat;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.impl.SardineException;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;

/**
 * Dependency handler interface for SuperNanny.
 * 
 * All dependencies call the handler to fetch them.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author David Santiago <dsantiago@tuenti.com> Adding ArtifactInfo
 */
public class ArchiveStrategy {
	public static final String TAR_XZ_EXT = ".tar.xz";
	public static final String TAR_BZ2_EXT = ".tar.bz2";
	public static final String TAR_GZ_EXT = ".tar.gz";
	@Inject
	protected Logger l;
	@Inject
	Util util;

	public void fetch(String uri, String fileName, File checkoutFolder) throws IOException {
		init(checkoutFolder);

		// download the dependency to a temp file
		File f = download(uri, fileName);

		// uncompress the archive to the proper folder
		decompress(checkoutFolder, f, fileName);
	}

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
	public void init(File depFolder) {
		if (depFolder.exists()) {
			try {
				util.deleteDir(depFolder);
			} catch (IOException e) {
			}
		}
		depFolder.mkdirs();
	}

	/**
	 * Get the dependency locally.
	 * 
	 * @param name
	 *            the name of the dependency.
	 * @param uri
	 *            URI of the dependency.
	 * @param version
	 *            of the dependency to fetch.
	 * @return version downloaded.
	 * @throws IOException
	 */
	public File download(String uri, String filename) throws IOException {
		File tmpFile = util.getTmpFile();

		URL depUrl = null;
		try {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(uri);
			stringBuilder.append(filename);
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

		return tmpFile;
	}

	public void decompress(File depFolder, File tmpFile, String originalFilename)
			throws IOException {
		String extractOpts;
		if (originalFilename.endsWith(TAR_GZ_EXT)) {
			extractOpts = " zxpf ";
		} else if (originalFilename.endsWith(TAR_BZ2_EXT)) {
			extractOpts = " jxpf ";
		} else if (originalFilename.endsWith(TAR_XZ_EXT)) {
			extractOpts = " Jxpf ";
		} else {
			throw new RuntimeException("Unsupported format " + originalFilename);
		}

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("tar ");
		stringBuilder.append(extractOpts);
		stringBuilder.append(tmpFile.toString());
		stringBuilder.append(" -C ");
		stringBuilder.append(depFolder.getAbsolutePath());
		// untar the file
		util.readProcess(stringBuilder.toString());
	}

	public void publish(File depFolder, String uri, String name, String version, String suffix,
			String extension) throws IOException {

		if (suffix.length() > 0) {
			version = version + Util.ARCHIVE_SUFFIX_DELIMITER + suffix;
		}
		String destinationName = MessageFormat.format("{0}{1}{2}{3}", name,
				Util.ARCHIVE_VERSION_DELIMITER, version, extension);

		File tmpFile = util.getTmpFile();
		compress(depFolder, tmpFile, extension);
		upload(uri, destinationName, tmpFile);

		l.info(MessageFormat.format("Package published to {0}{1}{2}{3}{4}", uri, name,
				Util.ARCHIVE_VERSION_DELIMITER, version, extension));
	}

	public void upload(String uri, String destinationName, File tmpFile)
			throws FileNotFoundException, IOException {
		InputStream fis = new FileInputStream(tmpFile);

		boolean wrongPassword;
		boolean fileAuthentication=false;
		int tries = 0;
		do {
			try {
				wrongPassword = false;
				String username = null;
				String password = null;
				try {
					String credentials[] = util.getCredentialsFromProperties();
					username = credentials[0];
					password = credentials[1];
					fileAuthentication=true;
				} catch (IOException e) {
					System.out.println("No credentials found in supernanny.properties!");
					username = util.readInput("[username:]");
					password = util.readPassword();
				}

				Sardine sardine = SardineFactory.begin(username, password);
				tries++;
				sardine.put(MessageFormat.format("{0}{1}", uri, destinationName), fis);
				return;
			} catch (SardineException e) {
				if (e.getStatusCode() == 401) {
					wrongPassword = true;
					System.err.println("Wrong credentials");
				} else {
					throw e;
				}
			}
		} while (!fileAuthentication && wrongPassword && tries < 3);
		
		throw new SuperNannyError("Error publishing artifact.");
	}

	private void compress(File depFolder, File tmpFile, String extension) throws IOException,
			SuperNannyError {
		String compressOpts;
		if (extension.equals(TAR_GZ_EXT)) {
			compressOpts = " -zcpf ";
		} else if (extension.equals(TAR_BZ2_EXT)) {
			compressOpts = " -jcpf ";
		} else if (extension.equals(TAR_XZ_EXT)) {
			compressOpts = " -Jcpf ";
		} else {
			throw new RuntimeException("Unsupported format " + extension);
		}

		// compress the folder
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("tar --exclude=.hg* --exclude=.git* ");
		stringBuilder.append(compressOpts);
		stringBuilder.append(tmpFile.getCanonicalPath().toString());
		stringBuilder.append(" -C ");
		stringBuilder.append(depFolder.getCanonicalPath().toString());
		stringBuilder.append(" .");
		util.readProcess(stringBuilder.toString());
	}
}
