/**
 * Dependency publishing for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny.resolution;

import java.io.IOException;
import java.util.Collection;

import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.repo.Repository;
import com.tuenti.supernanny.repo.artifacts.Export;

/**
 * Dependency publisher for SuperNanny.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class DepPublisher {
	/**
	 * Publish all exports
	 * 
	 * @param exports List of configured exports
	 * @param version Version to export for all exports
	 * @param p
	 * @throws IOException
	 */
	public void resolve(Collection<Export> exports, String version, CliParser p) throws IOException {
		String prefix = "";
		String suffix = "";
		if (p != null && p.prefix != null) {
			prefix = p.prefix;
		}
		if (p != null && p.suffix != null) {
			suffix = p.suffix;
		}
		for (Export export : exports) {
			Repository repository = export.getRepository();
			repository.publish(export, version, prefix, suffix);
		}
	}
}
