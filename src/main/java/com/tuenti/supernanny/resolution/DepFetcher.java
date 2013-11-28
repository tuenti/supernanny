/**
 * Dependency resolution for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Daniel Fanjul <dfanjul@tuenti.com>
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny.resolution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.dependencies.DependencyParser;
import com.tuenti.supernanny.dependencies.InvalidFormatException;
import com.tuenti.supernanny.dependencies.SNDependencyParser;
import com.tuenti.supernanny.repo.RepoProvider;
import com.tuenti.supernanny.repo.Repository;
import com.tuenti.supernanny.repo.artifacts.Artifact;
import com.tuenti.supernanny.repo.artifacts.ReqType;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.exceptions.ResolutionException;

/**
 * Dependency fetcher for SuperNanny.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Daniel Fanjul <dfanjul@tuenti.com>
 */
public class DepFetcher {

	private final class FetchTask implements Callable<FetchTask> {
		private final Set<String> expectedDirs;
		private final Artifact artifact;
		private boolean hadToFetch;

		private FetchTask(Set<String> expectedDirs, Artifact artifact) {
			this.expectedDirs = expectedDirs;
			this.artifact = artifact;
		}

		@Override
		public FetchTask call() throws Exception {
			hadToFetch = fetchArtifact(expectedDirs, artifact);
			return this;
		}
	}

	Util util;
	Logger l;
	ExecutorService executor;
	@Inject
	RepoProvider repoProvider;
	@Inject
	Resolver resolver;

	@Inject
	public DepFetcher(ExecutorService executor, Util util, Logger l) {
		this.util = util;
		this.l = l;
		this.executor = executor;
	}

	public void resolve(File projectPath, CliParser p) throws IOException {
		try {
			doResolve(p);
		} finally {
			executor.shutdown();
		}
	}

	private void doResolve(CliParser p) throws IOException {
		// fetch needed deps
		System.out.println("Init repos");
		DependencyParser dparser = new SNDependencyParser(ReqType.SW, util);
		List<Requirement> reqs = null;
		try {
			reqs = dparser.parseMultipleDepFiles(p);
		} catch (FileNotFoundException e) {
			System.err.println("Dep file not found, sure this is a supernanny repo?");
			System.exit(1);
		} catch (InvalidFormatException e) {
			System.err.println("Invalid format" + e);
			System.exit(1);
		}

		DependencyParser defaultParser = new SNDependencyParser(ReqType.GE, util);
		repoProvider.setDependencyParser(defaultParser);

		// init all known repos in parallel to speed things up
		try {
			repoProvider.warmUp(reqs, executor);
		} catch (Exception e) {
			System.err.println("Error initializing repos: " + e);
			System.exit(1);
		}

		final Set<String> expectedDirs = Collections.synchronizedSet(new HashSet<String>());
		try {
			System.out.println("Resolve dependencies");
			Set<Artifact> artifacts = resolver.resolve(reqs);

			System.out.println("Fetch dependencies");
			List<Future<FetchTask>> futures = new LinkedList<Future<FetchTask>>();
			for (final Artifact artifact : artifacts) {
				futures.add(executor.submit(new FetchTask(expectedDirs, artifact)));
				expectedDirs.add(artifact.getName());
			}

			List<String[]> rows = new ArrayList<String[]>();
			for (Future<FetchTask> future : futures) {
				try {
					FetchTask fetchTask = future.get();
					String prefix = "Ok";
					if (fetchTask.hadToFetch) {
						prefix = "Get";
					}
					Artifact artifact = fetchTask.artifact;
					rows.add(new String[] { prefix, artifact.getName(),
							artifact.getVersion().getVersionString(),
							artifact.getOrigin().getRepoType().toString(), artifact.getOriginUrl(), });
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			util.printColumns(rows, "  ", "  ", 1, true);

			if (!p.skipCleanup) {
				System.out.println("Cleanup");
				// delete all unexpected dirs in the deps folder
				File depDir = util.getDepsFolder();
				String[] deps = depDir.list();
				for (String dep : deps) {
					// skip symlinks, even though they might not be used anymore (this can be removed if the skipCleanup flag is used for all partial dep files)
					File d = new File(depDir, dep);
					if (!util.isSymlink(d) && !expectedDirs.contains(dep)) {
						// delete only directories, files like DEP.override will
						// not be deleted
						if (d.isDirectory()) {
							util.deleteDir(d);
						}
					}
				}
			}
		} catch (ResolutionException e) {
			System.out.println("Resolution error: " + e.getMessage());
			System.exit(1);
		}
	}

	private boolean fetchArtifact(Set<String> expectedDirs, Artifact artifact) throws IOException {
		Repository repository = artifact.getOrigin();
		boolean isUpdated = isUpdated(artifact);
		if (!isUpdated) {
			File destination = new File(util.getDepsFolder(), artifact.getName());
			repository.fetch(artifact, destination);
		}

		// store repo temp dir
		if (repository.getTmpDir() != null) {
			expectedDirs.add(repository.getTmpDir());
		}
		return !isUpdated;
	}

	private boolean isUpdated(Artifact artifact) {
		File folder = new File(util.getDepsFolder(), artifact.getName());
		try {
			if (!folder.exists()) {
				return false;
			}

			return artifact.getOrigin().isUpdated(artifact, folder);
		} catch (IOException e) {
			l.warning(MessageFormat.format(
					"SuperNanny dependency status file not found in {0}; refetching {1}.",
					folder.getAbsolutePath(), artifact.getName()));
			return false;
		}
	}
}
