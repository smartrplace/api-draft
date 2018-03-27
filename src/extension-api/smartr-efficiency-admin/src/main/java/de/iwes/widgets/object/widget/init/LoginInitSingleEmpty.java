package de.iwes.widgets.object.widget.init;

import javax.servlet.http.HttpSession;

import org.ogema.accesscontrol.Constants;
import org.ogema.accesscontrol.SessionAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.smarteff.admin.SpEffAdminApp;
import org.smartrplace.smarteff.admin.SpEffAdminController;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class LoginInitSingleEmpty extends ObjectInitSingleEmpty<SmartEffUserDataNonEdit> {
	
	private static final long serialVersionUID = 1L;
	protected final static Logger logger = LoggerFactory.getLogger(SpEffAdminApp.class);
	private final SpEffAdminController app;
	
	public LoginInitSingleEmpty(WidgetPage<?> page, String id, SpEffAdminController app) {
		super(page, id);
		this.app = app;
	}
	
	@Override
	public void init(OgemaHttpRequest req) {
        HttpSession session = req.getReq().getSession();
        SessionAuth sauth = (SessionAuth) session.getAttribute(Constants.AUTH_ATTRIBUTE_NAME);
        final String user = sauth.getName();
        // FIXME
        logger.debug("Page visited by user {}",user);
        for(SmartEffUserDataNonEdit userPattern: app.appConfigData.userDataNonEdit().getAllElements()) {
        	if(userPattern.ogemaUserName().getValue().equals(user)) {
        		getData(req).selectItem(userPattern);
        		return;
        	}
        }
		getData(req).selectItem(null);
	}
}
