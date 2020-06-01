package org.smartrplace.util.frontend.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ogema.accesscontrol.RestAccess;

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
	
//	private static final Logger logger = LoggerFactory.getLogger(RecordedDataServlet.class);
    private static final long serialVersionUID = 1L;
    //public static final String CONTEXT = "org.smartrplace.logging.fendodb.rest";
    //public static final String CONTEXT_FILTER = 
    //		"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + CONTEXT + ")";
	//private static final String SERVLET_ADDRESS = "org/smartrplace/apps/smartrplaceheatcontrolv2/userdatatest";

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
    
    @SuppressWarnings("deprecation")
	@Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    	//if(!req.getRequestURI().endsWith(SERVLET_ADDRESS)) return;
       	if(!checkAccessAllowedAndSendError(req, resp)) return;
    	resp.setCharacterEncoding("UTF-8");
    	resp.setContentType("application/json");
    	resp.addHeader("Access-Control-Allow-Origin", "*"); //CORS header
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
System.out.println("Adding CORS Header...");
        resp.addHeader("Access-Control-Allow-Headers", "*");
        //resp.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept");
        resp.addHeader("Access-Control-Allow-Credentials", "true");
System.out.println("Before doGET...");
    	try{
    		getUserServlet().doGet(req, resp);
    	} catch(NullPointerException e) {
    		throw new IllegalStateException("Error for req:"+javax.servlet.http.HttpUtils.getRequestURL(req), e);
    	}
System.out.println("After doGET...");
        /*else {
    		resp.getWriter().write("Servlet not accessible!");
        	resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	//resp.setStatus(HttpServletResponse.SC_OK);
    	}*/
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       	if(!checkAccessAllowedAndSendError(req, resp)) return;
    	resp.setCharacterEncoding("UTF-8");
    	resp.setContentType("application/json");
    	getUserServlet().doPost(req, resp);
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
    
    public boolean checkAccessAllowedAndSendError(final HttpServletRequest req, final HttpServletResponse resp) {
    	boolean isTest = isTestInstance(resp);
    	if(isTest)
    		return true;
    	RestAccess restAcc = getRESTAccess();
    	if(restAcc == null) {
    		sendError(resp);
    		return false;
    	}
    	try {
			return(restAcc.authenticate(req, resp) != null);
		} catch (ServletException | IOException e) {
			e.printStackTrace();
			sendError(resp);
			return false;
		}
    }
    
    public boolean isTestInstance(final HttpServletResponse resp) {
    	return (Boolean.getBoolean(getPropertyToCheck())||Boolean.getBoolean("org.smartrplace.util.frontend.servlet.istestinstance"));
    }
    public void sendError(final HttpServletResponse resp) {
		try {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "This servlet can only be used on testinstances! ");
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
