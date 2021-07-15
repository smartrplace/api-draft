package org.ogema.messaging.api;

/**
 *
 * @author jlapp
 */
public interface MessageTransport {
    
    /**
     * Sends a message.
     * 
     * @param msg Message to send.
     * 
     * @throws IllegalArgumentException if either recipients or message properties
     *         are null or empty.
     * @throws MessageTransportException if message could not be sent.
     */
    void sendMessage(MessageDTO msg);
    
    /**
     * Returns the type of address used by this transport.
     * 
     * @return address type supported by this transport.
     */
    String getAddressType();
    
    String getName();
    
}
