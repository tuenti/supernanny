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
import java.text.MessageFormat;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;

/**
 * Handler for 'delete' command.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class CliDeleteHandler implements CliHandler {
	@Inject
	Util util;
	@Inject CliParser p;

	@Override
	public String handle() {
		try {
			util.deleteDir(new File(util.getDepsFolder(), p.delete));
			System.out.println(MessageFormat.format("Dependency {0} deleted.", p.delete));
		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		return null;
	}
}