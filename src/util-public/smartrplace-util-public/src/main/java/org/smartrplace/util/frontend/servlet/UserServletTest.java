package org.smartrplace.util.frontend.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ogema.accesscontrol.RestAccess;
import org.ogema.accesscontrol.RestAccess.LoginViaNaturalUserChecker;
import org.ogema.core.model.ResourceList;
import org.ogema.model.user.NaturalPerson;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.resourcelist.ResourceListHelper;

/** Core class to provide testing variants for servlets based on widgets pages. This servlet
 * does only provide relevant information in the HTTP response when the property 
 * org.smartrplace.apps.heatcontrol.servlet.istestinstance is set true. This shall
 * NOT be the case in any productive instance as this would be a major security leak.
 * This servlet is accessible without any authentification.
*/
public abstract class UserServletTest extends HttpServlet {
	protected abstract UserServlet getUserServlet();
	protected RestAccess getRESTAccess() {
		return null;
	};
	
	/** This is the default property check that may be used also by other servlets if no special property is needed*/
	protected String getPropertyToCheck() {
		return "org.smartrplace.apps.heatcontrol.servlet.istestinstance";
	}
	
	/** Set this from external to enable checking for users allowing mobile access*/
	public static ResourceList<NaturalPerson> userData = null;
	
//	private static final Logger logger = LoggerFactory.getLogger(RecordedDataServlet.class);
    private static final long serialVersionUID = 1L;
    //public static final String CONTEXT = "org.smartrplace.logging.fendodb.rest";
    //public static final String CONTEXT_FILTER = 
    //		"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + CONTEXT + ")";
	//private static final String SERVLET_ADDRESS = "org/smartrplace/apps/smartrplaceheatcontrolv2/userdatatest";
	public static final String DEFAULT_LOGIN_USER_NAME = "TEST_INSTANCE_USER";

    @Override
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if(!checkAccessAllowedAndSendError(req, resp)) return;
    	resp.setCharacterEncoding("UTF-8");
    	resp.setContentType("application/json");
     	resp.addHeader("Access-Control-Allow-Origin", "*"); //CORS header
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        resp.addHeader("Access-Control-Allow-Headers", "*");
        //resp.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept");
        resp.addHeader("Access-Control-Allow-Credentials", "true");
    }
    
    protected String getUser(HttpServletRequest req, final HttpServletResponse resp) {
    	String user = checkAccessAllowedAndSendErrorToUser(req, resp);
    	if(user == null) return null;
    	//TODO: Perform mapping from REST user to natural user
    	//Perform mapping from REST user to natural user
        if(user.equals(DEFAULT_LOGIN_USER_NAME)) {
        	user = req.getParameter("user");
        } else {
        	if(user.endsWith("_rest"))
        		user = user.substring(0, user.length()-"_rest".length());
        	//else
        	//	user = "#REST#"+user;
        }
    	
		if(user == null || user.startsWith("[")) {
			String user1 = GUIUtilHelper.getUserLoggedInBase(req.getSession());
			if(user1 == null && (user == null || user.startsWith("["))) {
        		user = "master";
			} else
				user = user1;
		}
    	return user;
    }
    
    @SuppressWarnings("deprecation")
	@Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    	String user = getUser(req, resp);
    	if(user == null) {
    		sendError(resp);
    		return;
    	}
    	
    	//if(!req.getRequestURI().endsWith(SERVLET_ADDRESS)) return;
    	/*String user = checkAccessAllowedAndSendErrorToUser(req, resp);
    	if(user == null) return;
    	
    	//Perform mapping from REST user to natural user
        if(user.equals(DEFAULT_LOGIN_USER_NAME))
        	user = req.getParameter("user");
        else {
        	if(user.endsWith("_rest"))
        		user = user.substring(0, user.length()-"_rest".length());
        	else
        		user = "#REST#"+user;
        }
    	
		if(user == null || user.startsWith("["))
			user = GUIUtilHelper.getUserLoggedInBase(req.getSession());
    	 */
		resp.setCharacterEncoding("UTF-8");
    	resp.setContentType("application/json");
    	resp.addHeader("Access-Control-Allow-Origin", "*"); //CORS header
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        resp.addHeader("Access-Control-Allow-Headers", "*");
        //resp.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept");
        resp.addHeader("Access-Control-Allow-Credentials", "true");
		
		/*String test = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		System.out.println("Body : "+test);
		String auth = req.getAuthType();
		String ctype = req.getContentType();
		String contxtpath = req.getContextPath();
		String auth2 = req.getRemoteUser();
		Enumeration headers = req.getHeaderNames();
		Principal princ = req.getUserPrincipal();
		Enumeration auth3 = req.getHeaders("Authorization");
		Enumeration auth4 = req.getHeaders("Host");
		Enumeration auth5 = req.getHeaders("User-Agent");
		Enumeration auth6 = req.getHeaders("Accept");*/
        try{
    		getUserServlet().doGet(req, resp, user, true);
    	} catch(NullPointerException e) {
    		throw new IllegalStateException("Error for req:"+javax.servlet.http.HttpUtils.getRequestURL(req), e);
    	}
        /*else {
    		resp.getWriter().write("Servlet not accessible!");
        	resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	//resp.setStatus(HttpServletResponse.SC_OK);
    	}*/
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	String user = getUser(req, resp);
    	if(user == null) {
    		sendError(resp);
    		return;
    	}
    	//if(!checkAccessAllowedAndSendError(req, resp)) return;
    	
    	resp.setCharacterEncoding("UTF-8");
    	resp.setContentType("application/json");
    	getUserServlet().doPost(req, resp, user, true);
    	resp.addHeader("Access-Control-Allow-Origin", "*"); //CORS header
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        resp.addHeader("Access-Control-Allow-Headers", "*");
        resp.addHeader("Access-Control-Allow-Credentials", "true");
    }
    
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       	if(!checkAccessAllowedAndSendError(req, resp)) return;
    	super.doPut(req, resp);
    }
    
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       	if(!checkAccessAllowedAndSendError(req, resp)) return;
    	super.doDelete(req, resp);
    }
    
    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       	if(!checkAccessAllowedAndSendError(req, resp)) return;
    	
    	resp.setCharacterEncoding("UTF-8");
    	resp.setContentType("application/json");
    	resp.addHeader("Access-Control-Allow-Origin", "*"); //CORS header
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        resp.addHeader("Access-Control-Allow-Headers", "*");
        resp.addHeader("Access-Control-Allow-Credentials", "true");
    	super.doHead(req, resp);
    }
    
    /*@Override
    protected void doOptions(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
    	if(!isTestInstance(resp)) return;
    	super.doOptions(arg0, arg1);
    }
    
    @Override
    protected void doTrace(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
    	if(!isTestInstance(resp)) return;
    	super.doTrace(arg0, arg1);
    }*/
    
    /** 
     * 
     * @param req
     * @param resp
     * @return null if authentication failed, otherwise user name or test login
     */
    public boolean checkAccessAllowedAndSendError(final HttpServletRequest req, final HttpServletResponse resp) {
    	String userName = checkAccessAllowedAndSendErrorToUser(req, resp);
    	return userName != null;   	
    }
    public String checkAccessAllowedAndSendErrorToUser(final HttpServletRequest req, final HttpServletResponse resp) {
    	boolean isTest = isTestInstance(resp);
    	RestAccess restAcc = getRESTAccess();
    	if(restAcc == null) {
        	if(isTest)
        		return DEFAULT_LOGIN_USER_NAME;
    		sendError(resp);
    		return null;
    	}
    	try {
    		final String userName;
    		if(userData != null) {
    			userName = restAcc.authenticateToUser(req, resp, false, new LoginViaNaturalUserChecker() {
					
					@Override
					public boolean isLoginViaNaturalUserAllowed(String userName) {
						NaturalPerson ud = ResourceListHelper.getNamedElementFlex(userName, userData);
						if(ud != null && (!ud.restAccessDisabled().getValue()))
							return true;
						return false;
					}
				});
    		} else
    			userName = restAcc.authenticateToUser(req, resp, false);
 			if(userName == null) {
 				if(isTest)
 					return DEFAULT_LOGIN_USER_NAME;
 				else {
 					sendError(resp);
 					return null;
 				}
 			}
    		return userName;
		} catch (ServletException | IOException e) {
	    	if(isTest)
	    		return DEFAULT_LOGIN_USER_NAME;
			e.printStackTrace();
			sendError(resp);
			return null;
		}
    }
    
    public boolean isTestInstance(final HttpServletResponse resp) {
    	return (Boolean.getBoolean(getPropertyToCheck())||Boolean.getBoolean("org.smartrplace.util.frontend.servlet.istestinstance"));
    }
    public void sendError(final HttpServletResponse resp) {
		try {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "REST Login failed!");
			//resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "This servlet can only be used on testinstances! ");
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
