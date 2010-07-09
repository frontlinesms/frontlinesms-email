/**
 * 
 */
package net.frontlinesms.email.pop;

import javax.mail.Message;

/**
 * Class that processes messages received from a POP or IMAP email account.
 * @author Alex
 */
public interface PopImapMessageProcessor {
//> STATIC CONSTANTS

//> INSTANCE PROPERTIES

//> CONSTRUCTORS

//> ACCESSORS

//> INSTANCE METHODS
	/**
	 * Process an incoming email message 
	 * @param message the message to process
	 */
	public void processMessage(Message message);

//> STATIC FACTORIES

//> STATIC HELPER METHODS
}
