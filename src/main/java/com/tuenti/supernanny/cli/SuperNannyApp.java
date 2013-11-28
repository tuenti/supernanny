package com.tuenti.supernanny.cli;

import com.google.inject.Inject;
import com.sampullara.cli.Args;
import com.tuenti.supernanny.cli.handlers.CliCleanHandler;
import com.tuenti.supernanny.cli.handlers.CliDeleteHandler;
import com.tuenti.supernanny.cli.handlers.CliExportsHandler;
import com.tuenti.supernanny.cli.handlers.CliFetchHandler;
import com.tuenti.supernanny.cli.handlers.CliHandler;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.cli.handlers.CliPublishHandler;
import com.tuenti.supernanny.cli.handlers.CliStatusHandler;
import com.tuenti.supernanny.cli.handlers.CliVersionHandler;

public class SuperNannyApp {
	@Inject
	private CliParser p;
	@Inject
	private CliExportsHandler exportHandler;
	@Inject
	private CliFetchHandler fetchHandler;
	@Inject
	private CliPublishHandler publishHandler;
	@Inject
	private CliStatusHandler statusHandler;
	@Inject
	private CliDeleteHandler deleteHandler;
	@Inject
	private CliVersionHandler versionHandler;
	@Inject
	private CliCleanHandler cleanHandler;
	/**
	 * Run the correct entry point.
	 * 
	 * @param args
	 *            command line arguments.
	 */
	public void babysit(String[] args) {
		try {
			Args.parse(p, args);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			helpAndDie(1);
		}

		CliHandler requestHandler = null;

		if (p.help) {
			helpAndDie(0);
		} else if (p.exports) {
			requestHandler = exportHandler;
		} else if (p.version) {
			requestHandler = versionHandler;
		} else if (p.status) {
			requestHandler = statusHandler;
		} else if (p.fetch) {
			requestHandler = fetchHandler;
		} else if (p.publish) {
			requestHandler = publishHandler;
		} else if (p.delete != null) {
			requestHandler = deleteHandler;
		} else if (p.clean) {
			requestHandler = cleanHandler;
		} else {
			helpAndDie(0);
		}

		requestHandler.handle();
	}

	/**
	 * Show the help and stop the application.
	 * 
	 * @param exitCode
	 *            of the application.
	 */
	private void helpAndDie(int exitCode) {
		Args.usage(p);
		System.exit(exitCode);
	}
}