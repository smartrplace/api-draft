package org.ogema.tools.app.useradmin.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.ogema.core.administration.AdministrationManager;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.model.user.NaturalPerson;
import org.ogema.tools.app.useradmin.api.UserDataAccess;
import org.ogema.tools.app.useradmin.config.MessagingAddress;
import org.ogema.tools.app.useradmin.config.UserAdminData;
import org.ogema.tools.resource.util.ResourceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.resourcelist.ResourceListHelper;

/**
 *
 * @author jlapp
 */
@Component(service = Application.class)
public class UserDataAccessImpl implements UserDataAccess, Application {
    
    final static String TOPLEVELNAME = "userAdminData";
    final static String MESSAGINGDECORATOR = "messagingAddresses";
    final static String PROPERTIESDECORATOR = "properties";
    
    ApplicationManager appman;
    AdministrationManager admin;
    UserAdminData data;
    
    BundleContext ctx;
    ServiceRegistration<UserDataAccess> sreg;
    
    @Activate
    void activate(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public void start(ApplicationManager appManager) {
        this.appman = appManager;
        this.admin = appman.getAdministrationManager();
        sreg = ctx.registerService(UserDataAccess.class, this, null);
    }
    
    @Override
    public void stop(AppStopReason reason) {
        if (sreg != null) {
            sreg.unregister();
        }
    }
    
    @Override
    public List<String> getAllUsers() {
        return appman.getAdministrationManager().getAllUsers()
                .stream().map(UserAccount::getName).collect(Collectors.toList());
    }
    
    @Override
    public synchronized Resource getUserPropertyResource(String userId) {
        if (userId == null || !getAllUsers().contains(userId)) {
            throw new IllegalArgumentException("invalid user ID: " + userId);
        }
        if (data == null) {
            data = appman.getResourceManagement().createResource(TOPLEVELNAME, UserAdminData.class);
            data.activate(false);
        }
    	return GUIUtilHelper.getOrCreateUserPropertyResource(userId, data.userData());
    }
    
    private ResourceList<MessagingAddress> getAddressList(String userId) {
        Resource userRes = getUserPropertyResource(userId);
        @SuppressWarnings("unchecked")
        ResourceList<MessagingAddress> l = userRes.getSubResource(MESSAGINGDECORATOR, ResourceList.class);
        l.create();
        l.activate(false);
        if (l.getElementType() == null) {
            l.setElementType(MessagingAddress.class);
        }
        return l;
    }
    
    @Override
    public List<MessagingAddress> getMessagingAddresses(String userId, String addressType) {
        ResourceList<MessagingAddress> addresses = getAddressList(userId);
        return addresses.getAllElements().stream()
                .filter(a -> addressType == null || addressType.equalsIgnoreCase(a.addressType().getValue()))
                .collect(Collectors.toList());
    }
    
    @Override
    public synchronized void addMessagingAddress(String userId, String addressType, String address) {
        if (userId == null || addressType == null || address == null) {
            throw new IllegalArgumentException("null argument");
        }
        if (getMessagingAddresses(userId, addressType).stream()
                .filter(addr -> address.equals(addr.address().getValue())).findAny().isPresent()) {
            return;
        }
        @SuppressWarnings("unchecked")
        ResourceList<MessagingAddress> l = getAddressList(userId);
        String resname = ResourceUtils.getValidResourceName(addressType + "/" + address);
        MessagingAddress newAddr = l.getSubResource(resname) != null
                ? l.add() //name clash
                : l.getSubResource(resname, MessagingAddress.class);
        newAddr.address().create();
        newAddr.addressType().create();
        newAddr.address().setValue(address);
        newAddr.addressType().setValue(addressType);
        newAddr.activate(true);
    }
    
    @Override
    public boolean removeMessagingAddress(String userId, String addressType, String address) {
        if (userId == null || addressType == null || address == null) {
            throw new IllegalArgumentException("null argument");
        }
        Optional<MessagingAddress> addressRes = getMessagingAddresses(userId, addressType).stream()
                .filter(addr -> address.equals(addr.address().getValue())).findAny();
        addressRes.ifPresent(MessagingAddress::delete);
        return addressRes.isPresent();
    }
    
}
