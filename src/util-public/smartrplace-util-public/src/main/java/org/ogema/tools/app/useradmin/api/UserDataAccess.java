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

    /**
     * @param userId
     * @param addressType
     * @param address
     * @throws IllegalArgumentException if user does not exist or any argument
     * is null.
     */
    void addMessagingAddress(String userId, String addressType, String address);

    /*
     * @param userId
     * @param addressType 
     * @param address
     * @throws IllegalArgumentException if user does not exist or any argument is null.
     */
    boolean removeMessagingAddress(String userId, String addressType, String address);

    /**
     * @param userId user ID
     * @param addressType Return only addresses of the selected type, use
     * {@code null} to return all addresses.
     * @return MessagingAddresses of the requested type.
     * @throws IllegalArgumentException if userId == null or does not exist.
     */
    List<MessagingAddress> getMessagingAddresses(String userId, String addressType);

}
