/**
 * Command line handling.  
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.cli.handlers;

import com.google.inject.Inject;
import com.tuenti.supernanny.cli.ProjectStatus;

/**
 * Handler for 'status' command.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class CliStatusHandler implements CliHandler {
	@Inject private ProjectStatus status;
	@Inject CliParser p;

	@Override
	public String handle() {
		System.out.println(status.toString());
		return null;
	}
}