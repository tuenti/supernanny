/**
 * Dependency definition for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.strategy.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Comparator;
import java.util.TreeSet;

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
public abstract class DepStrategy implements FetchStrategy, PublishStrategy {
	@Inject
	protected Logger l;
	@Inject
	Util util;

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

	/**
	 * Get the most recent version that is compatible with the version prefix.
	 * 
	 * Version prefix can only contain a single wildcard, e.g. 1.3.* or 1.*.
	 * 
	 * The matcher must return the newest compatible version, newest defined by
	 * gnu version ordering standards.
	 * 
	 * @param depFolder
	 *            name of the project.
	 * @param uri
	 *            location of the project
	 * @param versionPrefix
	 *            version with a wildcard.
	 */
	public abstract String matchVersion(File depFolder, String uri,
			String versionPrefix);

	/**
	 * Sort versions. Return latest version.
	 * 
	 * @param versions
	 *            list of versions.
	 * @return most recent version.
	 * @throws IOException
	 */
	public String getLatestVersion(String[] versions) throws IOException {
		if (versions.length == 0) {
			System.err
					.println("Version could not be matched for one of the dependencies!");
			System.exit(1);
		}
		if (versions.length == 1) {
			return versions[0];
		}
		TreeSet<String> sortedVersions = new TreeSet<String>(
				new Comparator<String>() {
					public int compare(String s1, String s2) {
						if (s1.equals(s2)) {
							return 0;
						}
						String[] l1 = s1.split("[.,-]");
						String[] l2 = s2.split("[.,-]");
						int bound = l1.length;
						if (l2.length > bound) {
							bound = l2.length;
						}
						for (int i = 0; i < bound; i++) {
							if (i >= l1.length) {
								return 1;
							} else if (i >= l2.length) {
								return -1;
							}
							String e1 = l1[i];
							String e2 = l2[i];
							Integer i1 = null;
							Integer i2 = null;

							try {
								i1 = Integer.parseInt(l1[i]);
							} catch (NumberFormatException e) {
							}

							try {
								i2 = Integer.parseInt(l2[i]);
							} catch (NumberFormatException e) {
							}

							if ((i1 == null) && (i2 == null)) {
								int tmp = e1.compareTo(e2);
								if (tmp == 0) {
									continue;
								} else {
									return tmp;
								}
							} else if (i1 == null) {
								return -1;
							} else if (i2 == null) {
								return 1;
							}
							if (i1 != i2) {
								int tmp = i1.compareTo(i2);
								if (tmp == 0) {
									continue;
								} else {
									return -tmp;
								}
							}
						}
						return 0;
					}
				});

		for (String ver : versions) {
			sortedVersions.add(ver);
		}

		return sortedVersions.first();
	}

	/**
	 * Wrapper for process execution, getting and parsing the stderr/stdout, and
	 * redirecting it to the logger.
	 * 
	 * @param command
	 *            to execute.
	 * @return
	 * @throws IOException
	 *             if something goes wrong in process pipes.
	 */
	protected String readProcess(String command) throws SuperNannyError,
			IOException {
		l.info("Executing: " + command);

		Process p = null;
		try {
			p = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			l.log(Level.SEVERE,
					"Failed running the required program. Please install and check it and try again!",
					e);
			throw new SuperNannyError(e);
		}

		int exitVal = -1;
		try {
			exitVal = p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		BufferedReader input = new BufferedReader(new InputStreamReader(
				p.getInputStream()));

		StringBuffer sb = new StringBuffer("");

		// start an async read from stdout
		ProcessReader r = new ProcessReader(input);
		r.run();

		// read from stderr
		input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		sb.append("\n");
		String line = null;
		while ((line = input.readLine()) != null) {
			sb.append(line + "\n");
		}

		// wait for the stdout thread to finish and append the result
		try {
			r.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String stdOut = r.getOutput();
		sb.append(stdOut);

		l.fine(stdOut);
		if (exitVal != 0) {
			System.out.println("");
			System.err.println("Supernanny's child died with an error; terminating...");
			throw new SuperNannyError(MessageFormat.format(
					"Error executing: {0}; output: {1}; exit code {2}",
					command, sb.toString(), exitVal));
		}

		return stdOut;
	}

	/**
	 * Get the latest version.
	 * 
	 * @param depFolder
	 * @param uri
	 * @param version
	 * @return
	 */
	public abstract String getLatestVersion(String name, String uri);
}

/**
 * Simple thread that reads from bufferereade
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
class ProcessReader extends Thread {
	BufferedReader r = null;
	StringBuffer sb = new StringBuffer("");

	public ProcessReader(BufferedReader r) {
		super();
		this.r = r;
	}

	@Override
	public void run() {
		String line = null;
		try {
			while ((line = r.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getOutput() {
		return sb.toString();
	}
}
