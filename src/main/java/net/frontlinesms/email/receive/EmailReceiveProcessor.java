/**
 * 
 */
package net.frontlinesms.email.receive;

import java.util.Date;

import javax.mail.Message;

/**
 * Class that processes messages received from a POP or IMAP email account.
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public interface EmailReceiveProcessor {
//> STATIC CONSTANTS

//> INSTANCE PROPERTIES

//> CONSTRUCTORS

//> ACCESSORS

//> INSTANCE METHODS
	/**
	 * Process an incoming email message 
	 * @param message the message to process
	 * @param date the date of reception FIXME this argument should be removed.  It is taken from the header of message
	 */
	public void processMessage(Message message, Date date);

//> STATIC FACTORIES

//> STATIC HELPER METHODS
}
