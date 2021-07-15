package org.ogema.messaging.api;

/**
 *
 * @author jlapp
 */
public abstract class MessagingConstants {
    
    public static final String ADDRESS_TYPE_FIREBASE_TOKEN = "firebase_token";
    public static final String ADDRESS_TYPE_FIREBASE_TOPIC = "firebase_topic";
    public static final String ADDRESS_TYPE_EMAIL = "email";
    
    /**
     * Service property specifying the implementation's address type.
     */
    public static final String SERVICE_PROP_ADDRESS = "ogema.messaging.addresstype";
    
    /**
     * Service property specifying the implementation's ID.
     */
    public static final String SERVICE_PROP_ID = "ogema.messaging.id";
    
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
