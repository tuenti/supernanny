/**
 * Exception for wrong handler types.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.strategy;

/**
 * Exception to handle wrong/non-defined dependency types.
 *  
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class DepStrategyException extends Exception {
	public DepStrategyException(String string) {
		super(string);
	}

	private static final long serialVersionUID = 1L;
}
