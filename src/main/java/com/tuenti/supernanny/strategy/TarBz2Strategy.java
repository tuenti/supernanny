/**
 * Dependency handler tar.gz for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.strategy;

/**
 * Dependency handler tar.gz for SuperNanny.
 * 
 * Only overrides the tar command to use bzip2(j) instead of gzip.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class TarBz2Strategy extends TarGzStrategy {
	@Override
	protected String getArchiveExtension() {
		return ".tar.bz2";
	}

	@Override
	protected String getArchiveCmd() {
		return "tar --exclude=.hg* --exclude=.git* -jcpf ";
	}

	@Override
	protected String getExtractCmd() {
		return "tar xvpfj ";
	}
}