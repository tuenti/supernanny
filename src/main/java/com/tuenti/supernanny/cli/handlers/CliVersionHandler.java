/**
 * Command line handling.  
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.cli.handlers;

import com.tuenti.supernanny.Util;

/**
 * Handler for 'version' command.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class CliVersionHandler implements CliHandler {

	@Override
	public String handle() {
		System.out.println(Util.VERSION);
		return null;
	}
}