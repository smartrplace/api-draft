package org.ogema.tools.app.useradmin.api;

import java.util.List;
import org.ogema.core.model.Resource;
import org.ogema.tools.app.useradmin.config.MessagingAddress;

/**
 *
 * @author jlapp
 */
public interface UserDataAccess {
    
    List<String> getAllUsers();
    
    //boolean isMachine(String userId);
    
    /**
     * @param userId user ID.
     * @return Parent resource for this user's properties.
     * @throws IllegalArgumentException if userId == null or does not exist.
     */
    Resource getUserPropertyResource(String userId);
    
    void addMessagingAddress(String userId, String messagingType, String address);
    
    boolean removeMessagingAddress(String userId, String messagingType, String address);
    
    /**
     * @param userId user ID
     * @param messagingType Return only addresses of the selected type, use {@code null} to return all addresses.
     * @return MessagingAddresses of the requested type.
     * @throws IllegalArgumentException if userId == null or does not exist.
     */
    List<MessagingAddress> getMessagingAddresses(String userId, String messagingType);
    
}
