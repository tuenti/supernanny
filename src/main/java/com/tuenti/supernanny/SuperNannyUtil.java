/**
 * Utility helper for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlecode.sardine.DavResource;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.dependencies.Dependency;
import com.tuenti.supernanny.dependencies.Dependency.DepType;
import com.tuenti.supernanny.dependencies.NoOpDependency;

/**
 * Utility helper for SuperNanny.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
@Singleton
public class SuperNannyUtil implements Util {
	@Inject
	Logger l;
	@Inject
	Provider<Dependency> depProvider;
	private File root;
	private Map<String, List<DavResource>> davCache = new HashMap<String, List<DavResource>>();

	@Override
	public File getTmpFile() {
		File file = new File(System.getProperty("java.io.tmpdir"),
				new BigInteger(130, new SecureRandom()).toString(32));
		file.deleteOnExit();
		return file;
	}

	@Override
	public String getDepsFolder() {
		return getFile(root, DEP_FOLDER).getAbsolutePath();
	}

	@Override
	public void deleteDir(File dir) {
		if (dir.isDirectory()) {
			for (File c : dir.listFiles())
				deleteDir(c);
		}
		dir.delete();
	}

	@Override
	public void stampProject(File depFolder, String uri, String version,
			DepType type) throws IOException {
		FileWriter fstream = new FileWriter(new File(depFolder,
				SUPERNANNY_VERSION_FILE));
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(depFolder.getName() + " " + type.toString() + " " + uri + " "
				+ version + " ");
		out.flush();
		out.close();
	}

	@Override
	public DepType getProjectDepType(File depRoot) throws IOException,
			NoOpDependency {
		Dependency projectDependancy = getProjectDependancy(depRoot);
		if (projectDependancy == null) {
			return null;
		} else {
			return projectDependancy.getType();
		}
	}

	@Override
	public Dependency getProjectDependancy(File depRoot) throws IOException,
			NoOpDependency {
		File statFile = new File(depRoot, SUPERNANNY_VERSION_FILE);

		if (statFile.exists()) {

			BufferedReader reader = new BufferedReader(new FileReader(statFile));
			String line = null;

			while ((line = reader.readLine()) != null) {
				// ignore empty and commented lines
				if (line.trim().startsWith(COMMENT_START_CHAR)
						|| line.trim().length() == 0) {
					continue;
				}
				String[] lineParts = line.split("\\s");
				return depProvider.get().init(DepType.valueOf(lineParts[1]),
						depRoot.getName(), lineParts[2], lineParts[3]);
			}
		} else {
			System.out
					.println(MessageFormat
							.format("\n\tBroken dependency detected in {0}: Status file not found.\n",
									depRoot.getAbsolutePath()));
		}
		return null;
	}

	@Override
	public LinkedList<Dependency> parseMultipleDepFiles(Iterable<File> depsFile) {
		LinkedList<Dependency> mergedDeps = new LinkedList<Dependency>();
		for (File file : depsFile) {
			mergedDeps.addAll(parseDepsFile(file));
		}
		return mergedDeps;
	}

	@Override
	public LinkedList<Dependency> parseMultipleDepFiles(CliParser p) {
		LinkedList<File> depFiles = new LinkedList<File>();
		if (p.depfile == null) {
			p.depfile = Util.DEP_FILE;
		}

		if (p.depfile.contains(",")) {
			for (String depSource : p.depfile.split(",")) {
				depFiles.add(new File(depSource));
			}
		} else {
			depFiles.add(new File(p.depfile));
		}

		return parseMultipleDepFiles(depFiles);
	}

	@Override
	public LinkedList<Dependency> parseDepsFile(File depsFile) {
		LinkedList<Dependency> deps = new LinkedList<Dependency>();

		int line = 0;
		String[] depParts = null;
		String currentLine = null;
		try {
			for (String strLine : lineByLine(depsFile)) {
				currentLine = strLine;
				++line;

				depParts = strLine.split("\\s");

				// try to parse the file line
				// if cannot, die and report (error in file syntax )
				Dependency dep = null;
				try {
					dep = depProvider.get().init(DepType.valueOf(depParts[1]),
							depParts[0], depParts[2], depParts[3], depsFile);
					deps.add(dep);
					fixVersion(dep); // if contains *
				} catch (IllegalArgumentException e) {
					String message = MessageFormat.format(
							"Wrong type: {0}; must be one of {1}", depParts[1],
							Arrays.toString(DepType.values()));
					l.warning(message);
					System.out.println(message);
					System.exit(1);
				} catch (NoOpDependency e) {
					dep = null;
					try {
						dep = depProvider.get().init(DepType.GIT, depParts[0],
								depParts[2], depParts[3], depsFile);
					} catch (NoOpDependency e1) {
						// won't happen since type is git
					}
					dep.setDepType(DepType.NOOP);
					deps.add(dep);
					System.out.println(dep);
					System.out
							.println(MessageFormat
									.format("Dependency {0} is set to do no operation; skipping...",
											depParts[0]));
				}
			}
		} catch (FileNotFoundException e) {
			l.info("Deps file not found - not a supernanny project? Expected to find "
					+ depsFile.toString());
			return new LinkedList<Dependency>();

		} catch (IOException e) {
			l.info("Error in deps file : " + depsFile.toString());
			System.exit(1);
		} catch (ArrayIndexOutOfBoundsException e) {
			String msg = MessageFormat
					.format("Error in deps file : {0}\nIn line {1}, not enough parameter for dependency {2}, expected format:\n\n\t<name> <type> <uri> <version>\n\nactual entry:\n\n\t{3}\n\n",
							depsFile.toString(), line, depParts[1], currentLine);
			l.info(msg);
			System.out.println(msg);

			System.exit(1);
		}

		return deps;
	}

	private void fixVersion(Dependency dep) {
		if (dep.getVersion().equals("*")) {
			dep.setVersion(dep.getLatestVersion());
		} else if (dep.getVersion().contains("*")) {
			dep.setVersion(dep.matchVersion());
		}
	}

	@Override
	public LinkedList<Dependency> parseExportsFile(File exportsFile) {
		LinkedList<Dependency> deps = new LinkedList<Dependency>();

		try {
			for (String strLine : lineByLine(exportsFile)) {
				String[] depParts = strLine.split("\\s");

				Dependency dep;
				try {
					dep = depProvider.get().init(DepType.valueOf(depParts[1]),
							depParts[0], depParts[2], null);
				} catch (NoOpDependency e) {
					throw new SuperNannyError(
							"Cannot specify NOOP for exports!");
				}

				// check if the publish folder is set, if not assume "."
				if (depParts.length == 4) {
					dep.setDepFolder(new File(depParts[3]));
				}

				deps.add(dep);
			}
		} catch (FileNotFoundException e) {
			l.info("Export file not found - not a supernanny project? Expected to find "
					+ exportsFile.toString());
			System.exit(1);
		} catch (IOException e) {
			l.info("Error in exports file : " + exportsFile.toString());
			System.exit(1);
		} catch (ArrayIndexOutOfBoundsException e) {
			l.info("Error in exports file : " + exportsFile.toString());
			System.exit(1);
		}

		return deps;
	}

	@Override
	public Iterable<String> lineByLine(File f) throws IOException {
		Vector<String> lines = new Vector<String>();
		FileInputStream fstream = new FileInputStream(f);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		while ((strLine = br.readLine()) != null) {
			// ignore comment lines
			strLine = strLine.trim();
			if (strLine.length() == 0 || strLine.startsWith(COMMENT_START_CHAR)) {
				continue;
			}
			lines.add(strLine);
		}

		br.close();
		in.close();
		fstream.close();

		return lines;
	}

	@Override
	public String readInput(String message) throws IOException {
		System.out.print(message);
		return new BufferedReader(new InputStreamReader(System.in)).readLine();
	}

	@Override
	public String implodeArray(String[] inputArray, String glueString) {
		/** Output variable */
		String output = "";

		if (inputArray.length > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(inputArray[0]);

			for (int i = 1; i < inputArray.length; i++) {
				sb.append(glueString);
				sb.append(inputArray[i]);
			}

			output = sb.toString();
		}

		return output;
	}

	@Override
	public File getFile(File projectPath, String depFile) {
		return new File(projectPath, depFile);
	}

	@Override
	public String getProjectVersion(File depRoot) throws IOException {
		Dependency projectDependancy = null;
		try {
			projectDependancy = getProjectDependancy(depRoot);
		} catch (NoOpDependency e) {
			// ignore NOOPS
		}
		if (projectDependancy == null) {
			return null;
		} else {
			return projectDependancy.getVersion();
		}
	}

	@Override
	public String getNextVersion(String format, String latest) {
		String[] latestParts = latest.split("[.,-]");
		String[] formatParts = format.split("[.,-]");

		if (latestParts.length < formatParts.length) {
			l.severe(MessageFormat
					.format("Entered format has too many parts -- current latest version is {0}, which has only {1} parts, while the given format {2} has {3}.",
							latest.toString(), latestParts.length, format,
							formatParts.length));
			System.exit(1);
		}

		// extract all separators
		String[] latestSeparators = new String[latestParts.length - 1];
		{
			int i = 0;
			for (char c : latest.toCharArray()) {
				if ("[.,-]".contains(c + "")) {
					latestSeparators[i++] = c + "";
				}
			}
		}

		StringBuilder nextVersion = new StringBuilder();
		boolean didIncrease = false;

		for (int i = 0; i < latestParts.length; i++) {
			if (didIncrease) {
				if (formatParts.length > i) {
					l.severe("Given format is not correct; cannot contain anything after the first +.");
					System.exit(1);
				}
				// already increased, just pad 0
				nextVersion.append(0);
			} else if (formatParts[i].equals("x")) {
				// use
				nextVersion.append(latestParts[i]);
			} else if (formatParts[i].equals("+")) {
				// increase
				if (!didIncrease) {
					nextVersion.append(1 + Integer.parseInt(latestParts[i]));
					didIncrease = true;
				} else {
					l.severe("Given format is not correct; can only contain a single +.");
					System.exit(1);
				}
			} else {
				l.severe("Entered format is wrong; can only contain delimiters, + and x.");
				System.exit(1);
			}

			// add the format delimiter if not last iteration
			if (latestParts.length > i + 1) {
				nextVersion.append(latestSeparators[i]);
			}
		}

		return nextVersion.toString();
	}

	@Override
	public Map<String, String> parseForcedVersions(String[] versions) {
		HashMap<String, String> v = new HashMap<String, String>();

		for (String dep : versions) {
			// format: dep=ver, e.g. befw=1.3.2
			String[] parts = dep.split("=");
			v.put(parts[0], parts[1]);
		}

		return v;
	}

	@Override
	public String readPassword() throws IOException {
		String password = null;
		Console cons;
		char[] passwd;
		if ((cons = System.console()) != null
				&& (passwd = cons.readPassword("[%s]", "password:")) != null) {
			password = new String(passwd);
		}
		return password;
	}

	@Override
	public void setRoot(File projectPath) {
		root = projectPath;
	}

	@Override
	public File getRoot() {
		return root;
	}

	@Override
	public List<DavResource> getDavCahce(String uri) {
		return davCache.get(uri);
	}

	@Override
	public void setDavCache(String uri, List<DavResource> resources) {
		davCache.put(uri, resources);
	}
}