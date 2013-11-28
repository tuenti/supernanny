package com.tuenti.supernanny.repo.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.tuenti.supernanny.dependencies.DependencyParser;
import com.tuenti.supernanny.dependencies.SNDependencyParser;
import com.tuenti.supernanny.repo.artifacts.ArchiveArtifact;
import com.tuenti.supernanny.repo.artifacts.ReqType;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.util.Version;

public class IndexReader {
	public class UnexpectedField extends Exception {
		private static final long serialVersionUID = 6147428500401541048L;

		public UnexpectedField(String message) {
			super(message);
		}
	}

	public class EndOfFileException extends Exception {
		private static final long serialVersionUID = -6942852219032215043L;

	}

	public class KeyValue {
		public String key;
		public String value;

		public KeyValue(String key, String value) {
			super();
			this.key = key;
			this.value = value;
		}

	}

	class CountingBufferedReader{
		private int nline = 0;
		private BufferedReader in;
		public CountingBufferedReader(BufferedReader in) {
			this.in = in;
		}
		
		public String readLine() throws IOException {
			String line = in.readLine();
			nline++;
			return line;
		}

		public int getNline() {
			return nline;
		}
	}
	
	private CountingBufferedReader reader;
	private DependencyParser depParser;

	public IndexReader(BufferedReader reader) {
		super();
		this.reader = new CountingBufferedReader(reader);
		this.depParser = new SNDependencyParser(ReqType.GE, null);
	}

	private String readField(String name) throws EndOfFileException, UnexpectedField, IOException {
		String line = reader.readLine();
		if (line == null) {
			throw new EndOfFileException();
		}
		if (line.startsWith(name)) {
			return line.substring(name.length()).trim();
		}
		throw new UnexpectedField("Expected field \"" + name + "\" but got \"" + line + "\" in line " + reader.getNline());
	}

	private List<String> readDeps() throws EndOfFileException, UnexpectedField, IOException {
		String line;
		List<String> deps = new LinkedList<String>();
		while (true) {
			line = reader.readLine();
			if (line == null) {
				throw new EndOfFileException();
			}
			if ("".equals(line)) {
				return deps;
			}
			deps.add(line.trim());
		}
	}

	public List<ArchiveArtifact> parse() throws IOException {
		String name, version, file, md5;
		List<String> deps;
		List<ArchiveArtifact> artifacts = new LinkedList<ArchiveArtifact>();
		try {
			while (true) {
				try {
					name = readField("Name:");
				} catch (EndOfFileException e) {
					// expected end of file
					break;
				}
				version = readField("Version:");
				file = readField("File:");
				md5 = readField("MD5:");
				readField("Deps:");
				deps = readDeps();
				List<Requirement> reqs = depParser.parseDeps(deps);
				artifacts.add(new ArchiveArtifact(name, new Version(version), file, md5, null, reqs));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return artifacts;
	}
}
