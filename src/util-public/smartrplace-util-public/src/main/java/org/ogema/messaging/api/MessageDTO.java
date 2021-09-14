package org.ogema.messaging.api;

import java.util.List;
import java.util.Map;

/**
 *
 * @author jlapp
 */
public class MessageDTO {
    
    public MessageDTO() {
    }
    
    /**
     * Shallow copy constructor.
     * 
     * @param m Original message
     */
    public MessageDTO(MessageDTO m) {
        this.recipients = m.recipients;
        this.messageProperties = m.messageProperties;
    }
    
    /**
     * List of recipient addresses for this message. Address format is
     * specific to each MessageTransport implementation.
     */
    public List<String> recipients;
    
    /**
     * Message properties. A message is expected to contain at least a message
     * text (see {@link MessagingConstants} for properties), but may contain
     * additional properties specific to the transport implementation.
     */    
    public Map<String, Object> messageProperties;
    
    /**
     * For message systems that support messages with generic maps: should
     * the message properties defined in {@link MessagingConstants} which are
     * used in {@link #messageProperties} be propagated to that map? Default: false.
     */
    public boolean keepStandardPropertiesInGenericMap = false;
    
}
