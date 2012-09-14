/**
 * Command line handling.  
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.cli.handlers;

import java.io.File;
import java.text.MessageFormat;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.dependencies.Dependency;

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
	@Inject CliParser p;

	@Override
	public String handle() {
		System.out.println("# project's export targets\n");
		for (Dependency d : util.parseExportsFile(new File(Util.EXPORT_FILE))) {
			System.out.println(MessageFormat.format("\t{0}",
					d.toPublishString()));
		}
		return null;
	}
}