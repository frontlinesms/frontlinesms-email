/**
 * 
 */
package net.frontlinesms.email.receive;

/**
 * Exception thrown when there was a problem connecting to a POP email account to read messages.
 * @author Alex
 */
@SuppressWarnings("serial")
public class EmailReceiveException extends Exception {
	public EmailReceiveException(Throwable cause) {
		super(cause);
	}
}
