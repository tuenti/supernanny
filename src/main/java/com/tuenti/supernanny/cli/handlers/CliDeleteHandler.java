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

/**
 * Handler for 'status' command.
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
		util.deleteDir(new File(util.getDepsFolder(), p.delete));
		System.out.println(MessageFormat.format("Dependency {0} deleted.", p.delete));
		return null;
	}
}