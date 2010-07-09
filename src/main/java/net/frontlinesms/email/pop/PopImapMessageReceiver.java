/**
 * 
 */
package net.frontlinesms.email.pop;

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
 */
public class PopImapMessageReceiver {
//> STATIC CONSTANTS
	/** Folder name for the inbox */
	private static final String FOLDER_INBOX = "INBOX";
	/** Logging object for this class */
	private static Logger LOG = Logger.getLogger(PopImapMessageReceiver.class);
	
//> INSTANCE PROPERTIES
	/** Object that filters emails to reduce email spam. */
	private EmailFilter emailFilter;
	/** Object that will process received messages. */
	private final PopImapMessageProcessor processor;
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
	/** Protocol: POP or IMAP */
	private String protocol;

//> CONSTRUCTORS
	/**
	 * Creates a new {@link PopImapMessageReceiver}
	 * @param processor The {@link PopImapMessageProcessor} which processes incoming messages.
	 */
	public PopImapMessageReceiver(PopImapMessageProcessor processor) {
		if(processor == null) throw new IllegalArgumentException("Processor must not be null.");
		this.processor = processor;
	}
	
//> POP RECEIVE METHODS
	/**
	 * Blocking method which attempts to read incoming emails from a particular folder.
	 * @param folderName The name of the folder to read emails from
	 * @throws PopReceiveException 
	 */
	public void receive(String folderName) throws PopReceiveException {
		LOG.trace("ENTER : " + hostUsername + "@" + hostAddress + ":" + hostPort);

		//Store store = PopUtils.getPopStore(hostAddress, hostUsername, hostPort, hostPassword, useSsl);
		Store store = PopImapUtils.getStore(hostAddress, hostUsername, hostPort, hostPassword, useSsl, protocol);
		Folder folder = null;

		try {
			LOG.trace("Connecting to email store: " + hostAddress + ":" + hostPort);
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
//				if (this.lastCheck != null && this.lastCheck < message.getReceivedDate().getTime()) {
//					break;
//				}
					if(emailFilter == null || emailFilter.accept(message)) {
						if(emailFilter != null) LOG.info("Email accepted by filter.  Beginning processing.");
						processor.processMessage(message);
					} else {
						LOG.info("Email rejected by filter.");
					}
			}
			
			this.lastCheck = System.currentTimeMillis();

			LOG.trace("EXIT : POP email account checked without error.");
		} catch(MessagingException ex) {
			LOG.error("Unable to connect to POP account.", ex);
			throw new PopReceiveException(ex);
		} finally {
			// Attempt to close our folder
			if(folder != null) try { folder.close(true); } catch(MessagingException ex) { LOG.warn("Error closing POP folder.", ex); }

			// Attempt to close the message store
			try { store.close(); } catch(MessagingException ex) { LOG.warn("Error closing POP store.", ex); }
			
		}	
	}

	/**
	 * Blocking methods that attempts to read messages from a POP email account.
	 * @throws PopReceiveException If there was a problem receiving messages with this object.
	 */
	public void receive() throws PopReceiveException {
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

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocol() {
		return protocol;
	}
}
