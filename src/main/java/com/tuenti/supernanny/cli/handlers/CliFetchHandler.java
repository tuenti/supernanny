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
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.resolution.DepFetcher;

/**
 * Handler for 'fetch' command.
 * 
 * Fetches dependencies.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class CliFetchHandler implements CliHandler {
	@Inject
	Logger l;
	@Inject CliParser p;
	private final DepFetcher fetcher;

	@Inject
	public CliFetchHandler(DepFetcher fetcher) {
		this.fetcher = fetcher;
	}

	@Override
	public String handle() {
		try {
			fetcher.resolve(new File("."), p);
		} catch (IOException e) {
			l.warning(e.getMessage());
			throw new SuperNannyError(e);
		}
		return null;
	}
}