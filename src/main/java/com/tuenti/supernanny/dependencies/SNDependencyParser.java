package com.tuenti.supernanny.dependencies;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.repo.artifacts.ReqType;
import com.tuenti.supernanny.repo.artifacts.Requirement;

public class SNDependencyParser implements DependencyParser {
	private static final Pattern spaces = Pattern.compile("\\s");
	private Util util;
	private ReqType defaultReqType;

	public class ReqVersion {
		public ReqType reqType;
		public String version;

		public ReqVersion(ReqType reqType, String version) {
			super();
			this.reqType = reqType;
			this.version = version;
		}
	}

	public SNDependencyParser(ReqType defaultReqType, Util util) {
		super();
		this.defaultReqType = defaultReqType;
		this.util = util;
	}

	@Override
	public List<Requirement> parseMultipleDepFiles(Iterable<File> depsFile) throws IOException,
			InvalidFormatException {
		LinkedList<Requirement> mergedDeps = new LinkedList<Requirement>();
		Set<String> seen = new HashSet<String>();
		for (File file : depsFile) {
			if (file.exists()) {
				for (Requirement r : parseDepsFile(file)) {
					if (!seen.contains(r.getName())) {
						mergedDeps.add(r);
						seen.add(r.getName());
					}
				}
			}
		}
		return mergedDeps;
	}

	@Override
	public List<Requirement> parseMultipleDepFiles(CliParser p) throws IOException,
			InvalidFormatException {
		LinkedList<File> depFiles = new LinkedList<File>();
		if (p.depfile == null) {
			p.depfile = Util.DEP_FILE;
		}

		for (String depSource : p.depfile.split(",")) {
			depFiles.add(new File(depSource));
		}
		return parseMultipleDepFiles(depFiles);
	}

	public List<Requirement> parseDeps(Iterable<String> lines) throws InvalidFormatException {
		int line = 0;
		String[] depParts = null;
		String currentLine = null;
		List<Requirement> reqs = new LinkedList<Requirement>();
		for (String strLine : lines) {
			currentLine = strLine;
			++line;

			depParts = spaces.split(strLine);

			// try to parse the file line
			// if cannot, die and report (error in file syntax )
			try {
				ReqVersion ptype = tryToParseType(depParts[3], defaultReqType);
				Requirement req = new Requirement(depParts[0], ptype.reqType, ptype.version,
						RepositoryType.valueOf(depParts[1]), depParts[2]);
				reqs.add(req);
			} catch (IllegalArgumentException e) {
				String message = MessageFormat.format("Wrong type: {0}; must be one of {1}",
						depParts[1], Arrays.toString(RepositoryType.values()));
				throw new InvalidFormatException(message);
			} catch (ArrayIndexOutOfBoundsException e) {
				String msg = MessageFormat
						.format("Error in deps file : \nIn line {0}, not enough parameter for dependency {1}, expected format:\n\n\t<name> <type> <uri> <version>\n\nactual entry:\n\n\t{2}\n\n",
								line, depParts[1], currentLine);
				throw new InvalidFormatException(msg);
			}
		}
		return reqs;
	}

	private ReqVersion tryToParseType(String version, ReqType defaultReqType) {
		ReqType type = ReqType.fromStringStart(version);
		if (type != null) {
			// strip the reqtype from the version
			version = version.substring(type.toString().length());
		} else {
			type = defaultReqType;
		}
		return new ReqVersion(type, version);
	}

	@Override
	public List<Requirement> parseDepsFile(File depsFile) throws IOException,
			InvalidFormatException {
		return parseDeps(util.lineByLine(depsFile));
	}
}
