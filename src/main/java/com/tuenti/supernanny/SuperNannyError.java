/**
 * Error handling. 
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny;

/**
 * Error definition for critial failures.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class SuperNannyError extends Error {
	private static final long serialVersionUID = 1L;

	public SuperNannyError(Exception e) {
		super(e);
	}

	public SuperNannyError(String msg) {
		super(msg);
	}
}