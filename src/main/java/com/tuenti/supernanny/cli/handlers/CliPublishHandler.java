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
import java.util.Collection;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.dependencies.Dependency;
import com.tuenti.supernanny.resolution.DepPublisher;

/**
 * Handler for 'publish' command.
 * 
 * Publishes artifacts.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class CliPublishHandler implements CliHandler {
	@Inject
	Logger l;
	@Inject
	Util util;
	@Inject CliParser p;

	@Override
	public String handle() {
		try {
			File exportFile = new File(Util.EXPORT_FILE);

			if (!exportFile.exists()) {
				l.info(("Project export definitions not found in: " + new File(
						".").getAbsoluteFile().getName()));
				return null;
			}

			// export deps
			Collection<Dependency> deps = util.parseExportsFile(exportFile);
			for (Dependency d : deps) {

				if (!p.pretend) {
					String version = null;
					
					// if version is not implied by next version format, request input, otherwise calculate it
					if (p.next == null || p.next.equals("")) {
						version = util.readInput(MessageFormat.format(
								"Please input the version for {0} for {1}:\n{0}-",
								d.getName(), d.getType()));
					} else {
						version = util.getNextVersion(p.next, d.getLatestVersion());
					}

					d.setVersion(version);
				} else {
					System.out.println(MessageFormat.format(
							"\t# would publish {0}", d.toPublishString()
									.substring(2)));

				}
			}

			// publish only if not --pretend
			if (!p.pretend) {
				new DepPublisher().resolve(deps);
			}
		} catch (IOException e) {
			l.warning(e.getMessage());
			throw new SuperNannyError(e);
		}
		return null;
	}
}