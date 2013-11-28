/**
 * Dependency status.  
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.repo.artifacts.Requirement;

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

	public void printStatus() throws IOException {
		File deps = util.getDepsFolder();
		String[] libs = deps.list();
		List<String[]> rows = new ArrayList<String[]>();
		System.out.println("Current libraries:");
		System.out.println();
		String prefix = "  ";
		for (String libName : libs) {
			// ignore repos
			if (libName.startsWith(".repo")) {
				continue;
			}

			File lib = new File(deps, libName);
			if (lib.isFile()) {
				continue;
			}
			if (util.isSymlink(lib)) {
				rows.add(new String[] { lib.getName(), "*", "SYMLINK",
						"-> " + lib.getCanonicalPath() });
			} else {
				Requirement projectInfo = util.getProjectInfo(lib);
				if (projectInfo == null) {
					System.out.println(prefix + libName + " seems to be broken");
				} else {
					rows.add(new String[] { projectInfo.getName(),
							projectInfo.getVersion().getVersionString(),
							projectInfo.getRepoType().toString(), projectInfo.getRepo() });
				}
			}
		}
		util.printColumns(rows, prefix, "  ", 0, true);
	}
}
