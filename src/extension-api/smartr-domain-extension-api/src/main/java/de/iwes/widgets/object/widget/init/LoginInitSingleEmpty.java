package de.iwes.widgets.object.widget.init;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.ogema.accesscontrol.Constants;
import org.ogema.accesscontrol.SessionAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.extensionservice.ExtensionService;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class LoginInitSingleEmpty<T extends ExtensionUserDataNonEdit> extends ObjectInitSingleEmpty<T> {
	protected abstract List<T> getUsers(OgemaHttpRequest req);
	
	private static final long serialVersionUID = 1L;
	protected final static Logger logger = LoggerFactory.getLogger(ExtensionService.class);
	
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
        for(T userNE: getUsers(req)) {
        	if(userNE.ogemaUserName().getValue().equals(user)) {
        		getData(req).selectItem(userNE);
        		return;
        	}
        }
		getData(req).selectItem(null);
	}
}
