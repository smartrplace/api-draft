package org.smartrplace.tools.upload.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.AccessControlContext;
import java.security.DomainCombiner;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import org.ogema.accesscontrol.AppDomainCombiner;
import org.ogema.accesscontrol.PermissionManager;
import org.ogema.accesscontrol.UserRightsProxy;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.messaging.Message;
import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

@Property(name="osgi.http.whiteboard.servlet.pattern", value={"/org/smartrplace/tools/upload/servlet/*"})
@Service({Servlet.class, Application.class})
@Component(specVersion = "1.2", immediate = true)
public class FileUploadServerApp extends HttpServlet implements Application  {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(FileUploadServerApp.class);
	public static final String OTPNAME = "pw";
	public static final String OTUNAME = "user";
	private final DomainCombiner domainCombiner = new AppDomainCombiner();
	private long lastUpdate = Long.MIN_VALUE;
	private long cachedSize = Long.MIN_VALUE;
	private boolean warningActive = false;
	private boolean errorActive  =false;
	private ApplicationManager am;
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policy=ReferencePolicy.DYNAMIC)
	volatile OgemaGuiService widgetService;
	
	@Reference
	PermissionManager permMan;
	
	@Override
	public void start(ApplicationManager appManager) {
		this.am =appManager;
		if (widgetService != null) {
			widgetService.getMessagingService().registerMessagingApp(am.getAppID(), "File upload server app");
		}
		am.getLogger().info("Fileupload servlet started");
	}
	
	@Override
	public void stop(AppStopReason reason) {
		if (widgetService != null) {
			widgetService.getMessagingService().unregisterMessagingApp(am.getAppID());
		}
		this.am = null;
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.trace("POST request received");
		String path = req.getPathInfo();
		String user = checkAccess(req, path);
		if (user == null) {
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			logger.trace("Unauthorized request for user "+user+" to "+path);
			return;
		}
		try {
		MultipartConfigElement mce = new MultipartConfigElement("temp", 1024*1024*100, 1024*1024*100, 1024*1024*5);
		MultiPartInputStreamParser mpisp = new MultiPartInputStreamParser(req.getInputStream(), req.getContentType(), mce, am.getDataFile("temp"));
//	    Part filePart = req.getPart("file");
		logger.trace("Starting parts for path "+path);
		try {
			mpisp.getParts();
		} catch(IOException e) {
			//The file is probably locked on client, may the case for current log file
			logger.info("For path "+path+" IOException (probably file lock on client): "+e.getMessage());
			return;
		}
		logger.trace("Finished parts for path "+path);
		Part filePart = mpisp.getPart("file");
	    long size = filePart.getSize();
	    // not working yet
//	    if (!checkSize(size)) {
//	    	resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//	    	return;
//	    }
	    String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
	    if (fileName == null || fileName.isEmpty()) 
	    	fileName = "unspecified";
	    Path target = getValidFilename(path.substring(1), fileName, user);
	    try (InputStream is = filePart.getInputStream()) {
	    	Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
	    }
	    resp.setStatus(HttpServletResponse.SC_OK);
		logger.info("New file from user {} at {}",user,target);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		logger.trace("Finished request for path "+path);
	}
	
	// TODO react to deleted files
	private synchronized boolean checkSize(long size) {
		long now = System.currentTimeMillis();
		boolean recheck = cachedSize < 0 ||  (now - lastUpdate > 24 * 3600 * 1000); // check once per day at least
		if (recheck) {
			cachedSize = getSize();
			lastUpdate = now;
		}
		boolean newWarningActive = cachedSize + size > Constants.WARNING_UPLOAD_FOLDER_SIZE;
		boolean newErrorActive = cachedSize + size > Constants.MAX_UPLOAD_FOLDER_SIZE;
		if (!recheck && (newWarningActive && !warningActive || newErrorActive && !errorActive)) {
			cachedSize = getSize();
			lastUpdate= now;
			newWarningActive = cachedSize + size > Constants.WARNING_UPLOAD_FOLDER_SIZE;
			newErrorActive = cachedSize + size > Constants.MAX_UPLOAD_FOLDER_SIZE;
		}
		if (newErrorActive) {
			logger.error("File upload failed, exceeds storage size");
			if (recheck || !errorActive)
				sendMessage(MessagePriority.HIGH, cachedSize);
			errorActive = newErrorActive;
			return false;
		}
		if (recheck || (newWarningActive && !warningActive)) {
			sendMessage(warningActive ? MessagePriority.MEDIUM : MessagePriority.LOW, cachedSize + size);
			if (!recheck)
				logger.warn("Upload storage size of {} bytes exceeds configured warning limit of {} bytes",
						cachedSize+size,Constants.WARNING_UPLOAD_FOLDER_SIZE);
		}
		cachedSize += size;
		warningActive = newWarningActive;
		return true;
	}
	
	private void sendMessage(MessagePriority prio, long size) {
		if (widgetService == null) 
			return;
		Message message = new StorageSizeTooLargeMessage(prio, size);
		try {
			widgetService.getMessagingService().sendMessage(am, message);
		} catch (Exception e) {
			logger.error("Message sending failed",e);
		}
	}
	
	private static Long getSize() {
		boolean done = false;
		File f = new File(Constants.BASE_FOLDER);
		long currentSize = -1;
		do {
			try {
				currentSize = FileUtils.sizeOfDirectory(f);
				done = true;
			} catch (Exception ee) {  // e.g. if directory is modified during size check
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					return null; // will fail, this is ok
				}
			}
		} while (!done);
		return currentSize;
	}
	
	static Path getValidFilename(String folder, String filename, String user) throws IOException {
		String[] components = filename.split("\\.");
		String ending = null;
		if (components.length > 1) {
			ending = components[components.length-1];
			filename = filename.substring(0, filename.lastIndexOf('.'));
		}
		Path dir = Paths.get(Constants.BASE_FOLDER,user,folder);
		Files.createDirectories(dir);
		Path target;
		//do {
			StringBuilder newFilename = new StringBuilder();
			newFilename.append(filename); //.append('_').append(getTimestamp());
			if (ending != null)
				newFilename.append('.').append(ending);
			target = dir.resolve(newFilename.toString());
			
		//} while (Files.exists(target));
		return target; 
	}
	
	private static final String getTimestamp() {
		SimpleDateFormat sd = new SimpleDateFormat("yyyyMMddHHmmss");
		return sd.format(new Date(System.currentTimeMillis()));
	}
	
	
	// copied form OGEMA rest bundle
	private String checkAccess(HttpServletRequest req, String resourcePath) {
		/*
		 * Get The authentication information
		 */
		String usr = req.getParameter(OTUNAME);
		String pwd = req.getParameter(OTPNAME);

		if (usr == null) {
			logger.debug("REST access to {} without user name", resourcePath);
			return null;
		}
		if (pwd == null) {
			logger.debug("REST access to {} without user password", resourcePath);
			return null;
		}

		if (permMan.getAccessManager().authenticate(usr, pwd, false)) {
			UserRightsProxy urp = permMan.getAccessManager().getUrp(usr);
			if (urp == null) {
				logger.info("RestAccess denied for externally authenticated user " + usr);
				return null;
			}
			ProtectionDomain pda[] = new ProtectionDomain[1];
			pda[0] = urp.getClass().getProtectionDomain();
			if (checkAppAccess(pda, req.getMethod(), resourcePath)) {
				logger.info("RestAccess permitted for external authenticated user " + usr);
				return usr;
			}
		}
		return null;
	}
	
	private boolean checkAppAccess(ProtectionDomain pda[], String method, String path) {
		final AccessControlContext acc = new AccessControlContext(new AccessControlContext(pda), domainCombiner);
		permMan.setAccessContext(acc);
		return true;
	}
	
	private final static class StorageSizeTooLargeMessage implements Message {

		// error or warning
		private final MessagePriority priority;
		private final long size;
		
		public StorageSizeTooLargeMessage(MessagePriority priority, long size) {
			this.priority = priority;
			this.size = size;
		}
		
		@Override
		public String title(OgemaLocale locale) {
			return "Storage size exceeded";
		}

		@Override
		public String message(OgemaLocale locale) {
			StringBuilder msg = new StringBuilder();
			switch (priority) {
			case HIGH:
				msg.append("The upload storage area size has exceeded the configured maximum size of " + 
						Constants.MAX_UPLOAD_FOLDER_SIZE + " bytes. New files cannot be uploaded any more.");
				break;
			case MEDIUM:
				msg.append("The upload storage are size has exceeded the configured warning limit of "
						+ Constants.WARNING_UPLOAD_FOLDER_SIZE + " bytes. Maximum size: " 
						+ Constants.MAX_UPLOAD_FOLDER_SIZE + " bytes.");
				break;
			case LOW:
				msg.append("Info on the current upload storage area size. Configured warning limit: "
						+ Constants.WARNING_UPLOAD_FOLDER_SIZE + " bytes. Maximum size: " 
						+ Constants.MAX_UPLOAD_FOLDER_SIZE + " bytes.");
			}
			msg.append(" Current storage size: " + size + " bytes");
			return msg.toString();
		}

		@Override
		public String link() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MessagePriority priority() {
			return priority;
		}
		
	}
	
}
