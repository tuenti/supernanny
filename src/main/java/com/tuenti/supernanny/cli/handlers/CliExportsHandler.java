/**
 * Command line handling.  
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.cli.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.repo.artifacts.Export;

/**
 * Handler for 'exports' command.
 * 
 * Informs about targets exported by the project.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class CliExportsHandler implements CliHandler {
	@Inject
	Util util;
	@Inject
	CliParser p;

	@Override
	public String handle() {
		try {
			List<String[]> rows = new ArrayList<String[]>();
			for (Export d : util.parseExportsFile(new File(Util.EXPORT_FILE))) {
				rows.add(new String[] { d.getName(), d.getRepository().getRepoType().toString(),
						d.getRepository().getUri(), d.getFolder().toString() });
			}

			System.out.println("Project's export targets:");
			util.printColumns(rows, "  ", "  ", 1, true);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
}