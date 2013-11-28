/**
 * Command line handling.  
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.cli.handlers;

import java.io.IOException;

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
	@Inject CliParser p;
	@Inject ProjectStatus status;
	@Override
	public String handle() {
		try {
			status.printStatus();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}