/**
 * 
 */
package net.frontlinesms.email.pop;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.apache.log4j.Logger;

import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.pop3.POP3SSLStore;
import com.sun.mail.pop3.POP3Store;

/**
 * Utility methods for doing common actions on POP {@link Message}s.
 * @author Alex
 */
public class PopImapUtils {
//> STATIC CONSTANTS
	/** Logging object */
	private static Logger LOG = Logger.getLogger(PopImapUtils.class);
	/** MIME Type for plain text */
	private static final String MIMETYPE_TEXT_PLAIN = "text/plain";
	public static final String IMAP = "IMAP";
	public static final String POP = "POP3";
	public static final String SMTP = "SMTP";
	public static final String SMTPS = "smtps";
	private static final String TIMEOUT = "5000";

//> INSTANCE PROPERTIES

//> CONSTRUCTORS

//> ACCESSORS

//> INSTANCE HELPER METHODS

//> STATIC FACTORIES

//> STATIC HELPER METHODS
	/**
	 * Attempts to extract the message content from an email message.
	 * @param message
	 * @return The text content of the supplied email message, or <code>null</code> if none could be found.
	 * @throws MessagingException
	 * @throws IOException
	 */
	public static String getMessageText(javax.mail.Message message) throws MessagingException, IOException {
		Object messageContent = message.getContent();
		if (messageContent instanceof String) {
			// We've got a simple text message, so just return the text
			return (String)messageContent;
		} else if (messageContent instanceof Multipart) {
			// We've got a multipart message, so we need to check through the parts to find the
			// most text-like part.
			Multipart multipart = (Multipart) messageContent;
			String messageText = getMessageText(multipart, MIMETYPE_TEXT_PLAIN);
			if(messageText == null) {
				// We haven't found plain text.  The following should match any HTML-based text content.
				messageText = getMessageText(multipart, "text");
			}
		}
		return null;
	}

	/**
	 * Gets the text content from the supplied multipart message content of an email which matches the supplied MIME type.
	 * @param multipartContent
	 * @param acceptableMimeType The acceptable start of the MIME type for the content we are searching for.
	 * @return The message content of the supplied multipart email message content, or <code>null</code> if no acceptable content could be found.
	 * @throws MessagingException 
	 * @throws IOException 
	 */
	public static String getMessageText(Multipart multipartContent, String acceptableMimeType) throws MessagingException, IOException {
		int partCount = multipartContent.getCount();
		for(int i=0; i<partCount; ++i) {
			BodyPart part = multipartContent.getBodyPart(i);
			String contentType = part.getContentType();
			if (contentType.startsWith(acceptableMimeType)) {
				Object content = part.getContent();
				if (content instanceof String) {
					return (String)content;
				} else {
					LOG.warn("Body part with content type '" + contentType + "' had unexpected Content type: " + content.getClass().getCanonicalName());
				}
			}
		}
		return null;
	}

	/**
	 * Attempts to get a sane value for the sender of an email.
	 * @param message
	 * @return The first from address of the message, if one is supplied.  Otherwise, the first replyTo address, if one is supplied.  Otherwise an empty string.
	 * @throws MessagingException 
	 */
	public static String getSender(javax.mail.Message message) throws MessagingException {
		// First, check the "from" addresses
		// TODO only call getFrom() once?
		if (message.getFrom() != null) {
			for(Address address : message.getFrom()) {
				if(address != null) {
					String sender = address.toString();
					if(sender != null) return sender;
				}
			}
		}
		
		// TODO only call getReplyTo() once?
		if (message.getReplyTo() != null) {
			// No "from" address was found, so check the "reply to" addresses
			for(Address address : message.getReplyTo()) {
				if(address != null) {
					String sender = address.toString();
					if(sender != null) return sender;
				}
			}
		}
		// No address could be found, so return empty.
		return "";
	}
	
	/** @return {@link Store} for accessing the IMAP or POP account. */
	public static Store getStore(String host, String username, int hostPort, String password, boolean useSSL, String protocol) {
		// Create the properties
		Properties props = new Properties();
		
		props.setProperty("mail." + protocol + ".socketFactory.fallback", "false");
		props.setProperty("mail." + protocol + ".socketFactory.port", Integer.toString(hostPort));
		props.setProperty("mail." + protocol + ".port", Integer.toString(hostPort));
		props.setProperty("mail." + protocol + ".timeout", TIMEOUT);
		props.setProperty("mail." + protocol + ".connectiontimeout", TIMEOUT);
		props.setProperty("mail." + protocol + ".ssl.trust", "*");
		props.setProperty("mail." + protocol + ".starttls.enable", String.valueOf(useSSL));
		
		// Create session and URL
		Session session = Session.getInstance(props, null);
		session.setDebug(true);
		URLName url = new URLName(protocol, host, hostPort, "", username, password);
		
		// Create the store
		LOG.trace("Using SSL: " + useSSL);
		if (useSSL) {
			return (protocol.equals(IMAP) ? new IMAPSSLStore(session, url) : new POP3SSLStore(session, url));
		} else {
			return (protocol.equals(IMAP) ? new IMAPStore(session, url) : new POP3Store(session, url));
		}
	}
}
