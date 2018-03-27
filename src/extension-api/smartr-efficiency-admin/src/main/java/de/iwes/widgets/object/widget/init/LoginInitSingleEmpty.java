package de.iwes.widgets.object.widget.init;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.ogema.accesscontrol.Constants;
import org.ogema.accesscontrol.SessionAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.smarteff.admin.SpEffAdminApp;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public abstract class LoginInitSingleEmpty extends ObjectInitSingleEmpty<SmartEffUserDataNonEdit> {
	protected abstract List<SmartEffUserDataNonEdit> getUsers(OgemaHttpRequest req);
	
	private static final long serialVersionUID = 1L;
	protected final static Logger logger = LoggerFactory.getLogger(SpEffAdminApp.class);
	
	public LoginInitSingleEmpty(WidgetPage<?> page, String id) {
		super(page, id);
	}
	
	@Override
	public void init(OgemaHttpRequest req) {
        HttpSession session = req.getReq().getSession();
        SessionAuth sauth = (SessionAuth) session.getAttribute(Constants.AUTH_ATTRIBUTE_NAME);
        final String user = sauth.getName();
        // FIXME
        logger.debug("Page visited by user {}",user);
        for(SmartEffUserDataNonEdit userNE: getUsers(req)) {
        	if(userNE.ogemaUserName().getValue().equals(user)) {
        		getData(req).selectItem(userNE);
        		return;
        	}
        }
		getData(req).selectItem(null);
	}
}
