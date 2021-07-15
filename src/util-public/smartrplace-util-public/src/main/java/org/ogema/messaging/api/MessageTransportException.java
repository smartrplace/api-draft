package org.ogema.messaging.api;

/**
 *
 * @author jlapp
 */
public class MessageTransportException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public MessageTransportException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
