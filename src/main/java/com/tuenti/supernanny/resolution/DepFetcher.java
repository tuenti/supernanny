/**
 * Dependency resolution for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Daniel Fanjul <dfanjul@tuenti.com>
 */
package com.tuenti.supernanny.resolution;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.dependencies.Dependency;
import com.tuenti.supernanny.dependencies.Dependency.DepType;

/**
 * Dependency fetcher for SuperNanny.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Daniel Fanjul <dfanjul@tuenti.com>
 */
public class DepFetcher {
	Util util;
	Logger l;
	ExecutorService executor;

	@Inject
	public DepFetcher(ExecutorService executor, Util util, Logger l) {
		this.util = util;
		this.l = l;
		this.executor = executor;
	}

	public void resolve(File projectPath, CliParser p) throws IOException {
		Map<String, Future<File>> jobs = new HashMap<String, Future<File>>();

		// use overridden versions from CLI
		Map<String, String> ver = new HashMap<String, String>();
		if (p != null && p.force != null) {
			ver = util.parseForcedVersions(p.force);
		}

		// fetch needed deps
		LinkedList<Dependency> parseMultipleDepFiles = util
				.parseMultipleDepFiles(p);
		work(parseMultipleDepFiles, jobs, executor, ver, p);

		executor.shutdown();

		// delete untracked projects
		for (File f : util.getFile(null, util.getDepsFolder()).listFiles()) {
			if (f.isDirectory() && !jobs.containsKey(f.getName())) {
				util.deleteDir(f);
			}
		}
	}

	/**
	 * Recursive thread definition.
	 * 
	 * @author Goran Petrovic <gpetrovic@tuenti.com>
	 * @author Daniel Fanjul <dfanjul@tuenti.com>
	 */
	class FetchingThread implements Callable<File> {
		private final Dependency d;
		private final Map<String, Future<File>> jobs;
		private final ExecutorService executor;
		private final Map<String, String> ver;
		private final CliParser p;

		public FetchingThread(Map<String, Future<File>> jobs, Dependency d,
				ExecutorService executor, Map<String, String> ver, CliParser p) {
			this.d = d;
			this.jobs = jobs;
			this.executor = executor;
			this.ver = ver;
			this.p = p;
		}

		@Override
		public File call() throws Exception {
			File innerDep = d.fetch();

			work(util.parseDepsFile(innerDep), jobs, executor, ver, p);

			return innerDep;
		}
	}

	/**
	 * Execute the actual work of fetching.
	 * 
	 * @author Goran Petrovic <gpetrovic@tuenti.com>
	 * @author Daniel Fanjul <dfanjul@tuenti.com>
	 */
	protected void work(LinkedList<Dependency> parseDepsFile,
			Map<String, Future<File>> jobs, ExecutorService executor,
			Map<String, String> ver, CliParser p) {
		List<Future<File>> jobsList = new LinkedList<Future<File>>();

		synchronized (jobs) {
			for (Dependency d : parseDepsFile) {
				if (!jobs.containsKey(d.getName())) {
					if (ver.containsKey(d.getName())) {
						d.setVersion(ver.get(d.getName()));
					}

					if (p == null || !p.pretend && d.getType() != DepType.NOOP) {
						Future<File> submit = executor
								.submit(new FetchingThread(jobs, d, executor,
										ver, p));

						jobs.put(d.getName(), submit);
						jobsList.add(submit);
					} else {
						System.out.println(MessageFormat.format(
								"\t# would fetch {0}", d.toString()
										.substring(2)));
					}
				}
			}
		}

		for (Future<File> future : jobsList) {
			try {
				future.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (ExecutionException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}