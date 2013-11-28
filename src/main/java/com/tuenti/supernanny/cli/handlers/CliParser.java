/**
 * Command line handling.  
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny.cli.handlers;

import com.sampullara.cli.Argument;

/**
 * Defines the command line interface arguments.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class CliParser {
	@Argument(prefix = "", description = "Shows help")
	public boolean help;

	@Argument(prefix = "", description = "List artifacts exported by the project.")
	public boolean exports;

	@Argument(prefix = "--", description = "Don't actually execute the command, only show info.")
	public boolean pretend;

	@Argument(prefix = "--", description = "Format of the next version of format a.a.a where a is one of x or +. X means keep the current max value, + means increase, e.g. if current latest version is 3.1.4, with next of x.x.+ one gets 3.1.5, with next of x.+ one gets 3.2.4 and with next of +, one gets 4.1.4. Useful for pushiung new minor/major/patch versions.")
	public String next;

	@Argument(prefix = "--", description = "Prefix that will be used for all operations (e.g. --prefix beta).")
	public String prefix;

	@Argument(prefix = "--", description = "Version suffix for published artifacts")
	public String suffix;

	@Argument(prefix = "", description = "Publish a dependency")
	public boolean publish;
	
	@Argument(prefix = "", description = "Delete a dependency")
	public String delete;

	@Argument(prefix = "", description = "Delete all dependencies")
	public boolean clean;

	@Argument(prefix = "", description = "Fetch project dependencies")
	public boolean fetch;

	@Argument(prefix = "", description = "Shows dependency status")
	public boolean status;
	
	@Argument(alias="v", prefix = "-", description = "Shows version")
	public boolean version;

	@Argument(alias="f", prefix = "-", description = "Don't ask any questions - just do it")
	public boolean force;
	
	@Argument(prefix = "--", description = "Don't delete any unreferenced libraries after fetching")
	public boolean skipCleanup = false;
	
	@Argument(prefix = "--", description = "Path to the dependency file (defaults to .DEP)")
	public String depfile;

	@Argument(prefix = "--", description = "Skip verification of published artifacts checking the index")
	public boolean skipVerifyArtifacts = false;

	@Argument(prefix = "--", description = "Artifact verification timeout")
	public String verifyTimeout = "30";
	
	public void setPretend(boolean b) {
		this.pretend = b;
	}
}
