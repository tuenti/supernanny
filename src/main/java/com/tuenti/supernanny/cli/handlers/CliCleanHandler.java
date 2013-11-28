package com.tuenti.supernanny.cli.handlers;

import java.io.File;
import java.io.IOException;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;

/**
 * Delete the whole dependency folder
 */
public class CliCleanHandler implements CliHandler{
	@Inject
	Util util;
	
	@Override
	public String handle() {
		File depsFolder = util.getDepsFolder();
		try {
			util.deleteDir(depsFolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
