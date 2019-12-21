package org.smartrplace.util.frontend.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/** Core class to provide testing variants for servlets based on widgets pages. This servlet
 * does only provide relevant information in the HTTP response when the property 
 * org.smartrplace.apps.heatcontrol.servlet.istestinstance is set true. This shall
 * NOT be the case in any productive instance as this would be a major security leak.
 * This servlet is accessible without any authentification.
*/
@Component(
		service=Servlet.class,
		property= { 
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/org/smartrplace/apps/smartrplaceheatcontrolv2/userdatatest", // prefix to be set in ServletContextHelper
				//HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=" + UserServletTest.CONTEXT_FILTER
		}
)
public class UserServletTest extends HttpServlet {
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
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    	//if(!req.getRequestURI().endsWith(SERVLET_ADDRESS)) return;
    	if(!isTestInstance(resp)) return;
    	resp.setCharacterEncoding("UTF-8");
    	resp.setContentType("application/json");
    	UserServlet.getInstance().doGet(req, resp);
    	resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        /*else {
    		resp.getWriter().write("Servlet not accessible!");
        	resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	//resp.setStatus(HttpServletResponse.SC_OK);
    	}*/
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	if(!isTestInstance(resp)) return;
    	resp.setCharacterEncoding("UTF-8");
    	resp.setContentType("application/json");
    	UserServlet.getInstance().doPost(req, resp);
    	resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
    }
    
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	if(!isTestInstance(resp)) return;
    	super.doPut(req, resp);
    }
    
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	if(!isTestInstance(resp)) return;
    	super.doDelete(req, resp);
    }
    
    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	if(!isTestInstance(resp)) return;
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
    
    public boolean isTestInstance(final HttpServletResponse resp) {
    	if(!(Boolean.getBoolean(getPropertyToCheck())||Boolean.getBoolean("org.smartrplace.util.frontend.servlet.istestinstance"))) {
    		try {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "This servlet can only be used on testinstances! ");
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	return true;
    }
}
