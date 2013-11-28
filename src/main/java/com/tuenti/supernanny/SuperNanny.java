/**
 * SuperNanny cli entrypoint.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tuenti.supernanny.cli.SuperNannyApp;
import com.tuenti.supernanny.di.SuperNannyModule;

/**
 * Entry point for SuperNanny cli environment.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class SuperNanny {
	/**
	 * SuperNanny command-line entry point.
	 * 
	 * Handles command-line arguments and actions.
	 * 
	 * @param args
	 *            arguments.
	 */
	public static void main(String[] args) {
		/*
		 * Initialize dependency injection.
		 */
		Injector injector = Guice.createInjector(new SuperNannyModule());

		/*
		 * Configure the logger.
		 */
		Logger.getLogger("").setLevel(Level.OFF);

		/*
		 * Run SuperNanny.
		 */
		injector.getInstance(SuperNannyApp.class).babysit(args);
	}
}
