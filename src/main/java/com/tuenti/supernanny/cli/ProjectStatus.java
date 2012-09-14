/**
 * Dependency status.  
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.cli;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.dependencies.Dependency;
import com.tuenti.supernanny.dependencies.NoOpDependency;

/**
 * Creates the status message for the dependencies.
 * 
 * Informs about inconsistencies.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class ProjectStatus {
	@Inject
	Util util;
	@Inject
	CliParser p;

	public String toString() {
		LinkedList<Dependency> declaredDeps = util.parseMultipleDepFiles(p);
		LinkedList<Dependency> missingDeps = new LinkedList<Dependency>();
		LinkedList<Dependency> overridenDeps = new LinkedList<Dependency>();

		Map<String, Dependency> depsMap = new HashMap<String, Dependency>();

		for (Dependency d : declaredDeps) {
			if (depsMap.containsKey(d.getName())) {
				overridenDeps.add(d);
			} else {
				depsMap.put(d.getName(), d);
			}
		}

		StringBuilder b = new StringBuilder();
		b.append("# dependencies - what is currently present\n\n");
		File depsFolder = new File(util.getDepsFolder());
		if (depsFolder.exists()) {
			for (File f : depsFolder.listFiles()) {
				if (f.isDirectory()) {
					try {
						b.append("\t");
						Dependency dep;
						try {
							dep = util.getProjectDependancy(f);
							if (dep == null) {
								continue;
							}
							if (depsMap.containsKey(dep.getName())) {
								dep.setDepDefinitionFile(depsMap.get(
										dep.getName()).getDepDefinitionFile());
							} else {
								dep.setDepDefinitionFile(null);
							}
						} catch (NoOpDependency e) {
							dep = null; // skip, for NOOP
						}

						// if dependency is corrupted, skip it
						// warning will be issued
						if (dep == null) {
							continue;
						}
						if (declaredDeps.contains(dep)) {
							declaredDeps.remove(dep);
						} else {
							missingDeps.add(dep);
						}
						b.append(dep.toString());
						b.append("\n");
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		}

		if (declaredDeps.size() > 0 || missingDeps.size() > 0) {
			StringBuilder sb = new StringBuilder("\n");
			if (missingDeps.size() > 0) {
				sb.append("# removed - what is present currently, but was removed from dep files:\n");
			}
			for (Dependency dependency : missingDeps) {
				sb.append("\t");
				sb.append(dependency.toString().substring(1));
				sb.append("\n");
			}
			sb.append("\n");
			if (declaredDeps.size() > 0
					&& !overridenDeps.containsAll(declaredDeps)) {
				sb.append("# new - what is not present currently but appears in dep files\n");

				for (Dependency dependency : declaredDeps) {
					if (!overridenDeps.contains(dependency)) {
						sb.append("\t");
						sb.append(dependency.toString().substring(1));
						sb.append("\n");
					}
				}
			}
			b.append(sb.toString());
		}

		if (overridenDeps.size() > 0) {
			StringBuilder sb = new StringBuilder("\n");
			if (overridenDeps.size() > 0) {
				sb.append("# overriden  - what will not be used because it was overriden:\n");
			}

			for (Dependency dependency : overridenDeps) {
				sb.append("\t");
				sb.append(dependency.toString().substring(1));
				sb.append("\n");
			}
			b.append(sb.toString());
		}

		return b.toString();
	}
}