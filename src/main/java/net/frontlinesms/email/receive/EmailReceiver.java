/**
 * 
 */
package net.frontlinesms.email.receive;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.Flags.Flag;

import net.frontlinesms.email.EmailFilter;

import org.apache.log4j.Logger;

/**
 * Object that reads messages from a POP or IMAP email account.
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class EmailReceiver {
//> STATIC CONSTANTS
	/** Folder name for the inbox */
	private static final String FOLDER_INBOX = "INBOX";
	private static final String HEADER_DATE = "Date";
	
	/** Logging object for this class */
	private static Logger LOG = Logger.getLogger(EmailReceiver.class);
	
//> INSTANCE PROPERTIES
	/** Object that filters emails to reduce email spam. */
	private EmailFilter emailFilter;
	/** Object that will process received messages. */
	private final EmailReceiveProcessor processor;
	/** Flag indicating this should use SSL when connecting to the email server. */
	private boolean useSsl;
	/** Port to connect to on the POP server. */
	private int hostPort;
	/** Username on the POP server. */
	private String hostUsername;
	/** Password for accessing the POP server. */
	private String hostPassword;
	/** Address of the POP server. */
	private String hostAddress;
	/** Last check */
	private Long lastCheck;
	/** Protocol: POP3 or IMAP */
	private EmailReceiveProtocol protocol;

//> CONSTRUCTORS
	/**
	 * Creates a new {@link EmailReceiver}
	 * @param processor The {@link EmailReceiveProcessor} which processes incoming messages.
	 */
	public EmailReceiver(EmailReceiveProcessor processor) {
		if(processor == null) throw new IllegalArgumentException("Processor must not be null.");
		this.processor = processor;
	}
	
//> POP RECEIVE METHODS
	/**
	 * Blocking method which attempts to read incoming emails from a particular folder.
	 * @param folderName The name of the folder to read emails from
	 * @throws EmailReceiveException 
	 */
	public void receive(String folderName) throws EmailReceiveException {
		LOG.trace("ENTER : " + hostUsername + "@" + hostAddress + ":" + hostPort);

		//Store store = PopUtils.getPopStore(hostAddress, hostUsername, hostPort, hostPassword, useSsl);
		Store store = EmailReceiveUtils.getStore(hostAddress, hostUsername, hostPort, hostPassword, useSsl, protocol);
		Folder folder = null;

		try {
			LOG.trace("Connecting to " + protocol + " store: " + hostAddress + ":" + hostPort);
			store.connect();

			// Get a handle on the INBOX folder.
			folder = store.getDefaultFolder().getFolder(folderName);
			if(folder == null) throw new MessagingException("Got null handle for requested folder: '" + folderName + "'");

			try {
				LOG.trace("Attempting to open folder for read/write.");
				folder.open(Folder.READ_WRITE);
			} catch (MessagingException ex) {
				LOG.trace("Opening folder for Read/write failed.  Attempting to open folder for read only.");
				folder.open(Folder.READ_ONLY);
			}

			Message[] messages = folder.getMessages();
			// Loop over all of the messages
			for (Message message : messages) {
				if (protocol == EmailReceiveProtocol.POP3) {
					this.handlePopMessage(message);
				} else if (this.lastCheck == null || !message.getFlags().contains(Flag.SEEN)) {
					this.processMessage(message, message.getReceivedDate());
				}
			}

			LOG.trace("EXIT : " + protocol + " email account checked without error.");
		} catch(MessagingException ex) {
			LOG.error("Unable to connect to " + protocol + " account.", ex);
			throw new EmailReceiveException(ex);
		} finally {
			// Attempt to close our folder
			if(folder != null) try { folder.close(true); } catch(MessagingException ex) { LOG.warn("Error closing " + protocol + " folder.", ex); }

			// Attempt to close the message store
			try { store.close(); } catch(MessagingException ex) { LOG.warn("Error closing " + protocol + " store.", ex); }
			
		}	
	}
	
	private void handlePopMessage (Message message) {
		Date date = null;
		try {
			String[] dateHeader = message.getHeader(HEADER_DATE);
			if (dateHeader != null && dateHeader.length > 0) {
				DateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
				date = (Date)formatter.parse(dateHeader[0]);
			}
			
			message.setFlag(Flag.DELETED, true);
		} catch (MessagingException e) {
		} catch (ParseException e) { }
		
		if (this.lastCheck == null || date == null || date.after(new Date(this.lastCheck))) {
			this.processMessage(message, date);
		}
	}

	private void processMessage(Message message, Date date) {
		if(emailFilter == null || emailFilter.accept(message)) {
			if(emailFilter != null) LOG.info("Email accepted by filter.  Beginning processing.");
			processor.processMessage(message, date);
		} else {
			LOG.info("Email rejected by filter.");
		}
	}

	/**
	 * Blocking methods that attempts to read messages from a POP email account.
	 * @throws EmailReceiveException If there was a problem receiving messages with this object.
	 */
	public void receive() throws EmailReceiveException {
		this.receive(FOLDER_INBOX);
	}
	
//> ACCESSORS
	/**
	 * @return the useSsl
	 */
	public boolean isUseSsl() {
		return useSsl;
	}

	/**
	 * @param useSsl the useSsl to set
	 */
	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	/**
	 * @return the hostPort
	 */
	public int getHostPort() {
		return hostPort;
	}

	/**
	 * @param hostPort the hostPort to set
	 */
	public void setHostPort(int hostPort) {
		this.hostPort = hostPort;
	}

	/**
	 * @return the hostUsername
	 */
	public String getHostUsername() {
		return hostUsername;
	}

	/**
	 * @param hostUsername the hostUsername to set
	 */
	public void setHostUsername(String hostUsername) {
		this.hostUsername = hostUsername;
	}

	/**
	 * @return the hostPassword
	 */
	public String getHostPassword() {
		return hostPassword;
	}

	/**
	 * @param hostPassword the hostPassword to set
	 */
	public void setHostPassword(String hostPassword) {
		this.hostPassword = hostPassword;
	}

	/**
	 * @return the hostAddress
	 */
	public String getHostAddress() {
		return hostAddress;
	}

	/**
	 * @param hostAddress the hostAddress to set
	 */
	public void setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
	}

	public void setLastCheck(Long lastCheck) {
		this.lastCheck = lastCheck;
	}

	public Long getLastCheck() {
		return lastCheck;
	}

	public void setProtocol(EmailReceiveProtocol protocol) {
		this.protocol = protocol;
	}

	public EmailReceiveProtocol getProtocol() {
		return protocol;
	}
}
