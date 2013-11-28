/**
 * Utility helper for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.RepoProvider;
import com.tuenti.supernanny.repo.Repository;
import com.tuenti.supernanny.repo.artifacts.Export;
import com.tuenti.supernanny.repo.artifacts.ReqType;
import com.tuenti.supernanny.repo.artifacts.Requirement;

/**
 * Utility helper for SuperNanny.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
@Singleton
public class SuperNannyUtil implements Util {
	@Inject
	RepoProvider repoProvider;

	@Inject
	Logger l;
	private File root;

	@Inject
	CliParser p;

	@Override
	public File getTmpFile() {
		File file = new File(System.getProperty("java.io.tmpdir"), "SN_"
				+ new BigInteger(130, new SecureRandom()).toString(32));
		file.deleteOnExit();
		return file;
	}

	@Override
	public File getDepsFolder() {
		return new File(root, DEP_FOLDER);
	}

	@Override
	public void deleteDir(File dir) throws IOException {
		execProcess("rm -rf " + dir.getAbsolutePath());
	}

	@Override
	public void stampProject(String name, File depFolder, String uri, String version,
			RepositoryType type) throws IOException {
		FileWriter fstream = new FileWriter(new File(depFolder, SUPERNANNY_VERSION_FILE));
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(name + " " + type.toString() + " " + uri + " " + version + " ");
		out.flush();
		out.close();
	}

	@Override
	public List<Export> parseExportsFile(File exportsFile) throws IOException {
		LinkedList<Export> exports = new LinkedList<Export>();

		for (String strLine : lineByLine(exportsFile)) {
			String[] depParts = strLine.split("\\s");

			Repository repo = repoProvider
					.getRepo(RepositoryType.valueOf(depParts[1]), depParts[2]);
			String name = depParts[0];
			String folder = ".";
			if (depParts.length == 4) {
				folder = depParts[3];
			}

			exports.add(new Export(repo, name, new File(folder)));
		}

		return exports;
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
	public String[] getCredentialsFromProperties() throws IOException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(System.getProperty("user.home") + File.separator
				+ PROPERTIES_FILE));

		return new String[] { properties.getProperty("username"),
				properties.getProperty("password") };
	}

	@Override
	public boolean confirmYN(String message) throws IOException {
		if (p.force) {
			System.out.println(message + " - forced");
			return true;
		} else {
			String input = readInput(message + " [y/n]");

			return ("y".equals(input.trim().toLowerCase()));
		}
	}

	@Override
	public boolean isSymlink(File f) throws IOException {
		// Checking for symlinks in java is practically impossible
		return execProcess("test -h " + f.getAbsolutePath()) == 0;
	}

	@Override
	public Requirement getProjectInfo(File depRoot) throws IOException {
		File statFile = new File(depRoot, SUPERNANNY_VERSION_FILE);

		if (statFile.exists()) {

			BufferedReader reader = new BufferedReader(new FileReader(statFile));
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					// ignore empty and commented lines
					if (line.trim().startsWith(COMMENT_START_CHAR) || line.trim().length() == 0) {
						continue;
					}
					String[] lineParts = line.split("\\s");
					return new Requirement(lineParts[0], ReqType.EQ, lineParts[3],
							RepositoryType.valueOf(lineParts[1]), lineParts[2]);
				}
			} finally {
				reader.close();
			}
		}
		return null;
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
	@Override
	public String readProcess(String command) throws SuperNannyError, IOException {
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
			throw new SuperNannyError(e);
		}

		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

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
			e.printStackTrace();
		}
		String stdOut = r.getOutput();
		sb.append(stdOut);

		l.fine(stdOut);
		if (exitVal != 0) {
			throw new SuperNannyError(MessageFormat.format(
					"Error executing: {0}; output: {1}; exit code {2}", command, sb.toString(),
					exitVal));
		}

		return stdOut;
	}

	@Override
	public void printColumns(List<String[]> rows, String prefix, String colSep, int sortCol,
			boolean ascending) {
		sortRowsByColumn(rows, sortCol, ascending);
		printColumns(rows, prefix, colSep);
	}

	@Override
	public void sortRowsByColumn(List<String[]> rows, final int sortCol, final boolean ascending) {
		Collections.sort(rows, new Comparator<String[]>() {
			@Override
			public int compare(String[] o1, String[] o2) {
				int order = ascending ? 1 : -1;
				String left = "";
				String right = "";
				
				if (o1.length > sortCol && o1[sortCol] != null) {
					left = o1[sortCol];
				}

				if (o2.length > sortCol && o2[sortCol] != null) {
					right = o2[sortCol];
				}
				return order * left.compareTo(right);
			}
		});
	}

	@Override
	public void printColumns(List<String[]> rows, String prefix, String colSep) {
		if (rows.size() > 0) {
			int[] maxlengths = new int[100];

			// find maxlengths for each colums
			for (String[] cols : rows) {
				for (int i = 0; i < cols.length; i++) {
					if (cols[i].length() > maxlengths[i]) {
						maxlengths[i] = cols[i].length();
					}
				}
			}
			for (String[] cols : rows) {
				System.out.print(prefix);
				for (int i = 0; i < cols.length; i++) {
					if (maxlengths[i] == 0) {
						continue;
					}
					System.out.printf("%-" + maxlengths[i] + "s" + colSep, cols[i]);
				}
				System.out.println("");
			}
		}
	}

	@Override
	public int execProcess(String command) throws IOException {
		Process p = Runtime.getRuntime().exec(command);
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return p.exitValue();
	}
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
