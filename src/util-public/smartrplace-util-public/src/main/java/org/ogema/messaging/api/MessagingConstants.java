package org.ogema.messaging.api;

/**
 *
 * @author jlapp
 */
public abstract class MessagingConstants {
    
    /**
     * Service property specifying the implementation's messaging type.
     */
    public static final String SERVICE_PROP_MESSAGINGTYPE = "ogema.messaging.type";
    
    /**
     * Standard message property for the message sender ("{@value}").
     */
    public static final String FROM = "standardMessageProperty.from";
    
    /**
     * Standard message property for the message title ("{@value}").
     */
    public static final String TITLE = "standardMessageProperty.title";
    
    /**
     * Standard message property for the message text ("{@value}").
     */
    public static final String MESSAGE = "standardMessageProperty.message";
    
    private MessagingConstants() {}
    
}
