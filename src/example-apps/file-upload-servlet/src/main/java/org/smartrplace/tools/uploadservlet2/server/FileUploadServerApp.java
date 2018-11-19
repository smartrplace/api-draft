package org.smartrplace.tools.uploadservlet2.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DomainCombiner;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

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
import org.ogema.accesscontrol.PermissionManager;
import org.ogema.accesscontrol.RestAccess;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.gateway.EvalCollection;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.logging.fendodb.DataRecorderReference;
import org.smartrplace.logging.fendodb.FendoDbFactory;

import de.iwes.util.logconfig.EvalHelper;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.messaging.Message;
import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

@Property(name="osgi.http.whiteboard.servlet.pattern", value={"/org/smartrplace/tools/upload/servlet/*"})
@Service({Servlet.class, Application.class})
@Component(specVersion = "1.2")
public class FileUploadServerApp extends HttpServlet implements Application  {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(FileUploadServerApp.class);
	public static final String OTPNAME = "pw";
	public static final String OTUNAME = "user";
	private long lastUpdate = Long.MIN_VALUE;
	private long cachedSize = Long.MIN_VALUE;
	private boolean warningActive = false;
	private boolean errorActive  =false;
	private ApplicationManager am;
	
	final static String startString = "/home/user/ogemaCollect/rest/";
	final static int startIdx = startString.length();
	final static String resNameStart = "fos_lastrecv"; 
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policy=ReferencePolicy.DYNAMIC)
	volatile OgemaGuiService widgetService;
	
	@Reference
	private PermissionManager permMan;
	
	@Reference
	private RestAccess restAccess;
	
	// FIXME remove
	@Reference
	FendoDbFactory fendoFactory;
	
	volatile CountDownDelayedExecutionTimer fendoUpdateTimer = null;
	
	@Override
	public void start(ApplicationManager appManager) {
		this.am =appManager;
		if (widgetService != null) {
			widgetService.getMessagingService().registerMessagingApp(am.getAppID(), "File upload server app");
		}
		am.getLogger().info("Fileupload servlet started");
		
		//FIXME
		EvalCollection evalC = EvalHelper.getEvalCollection(am);
		TimeResource res = evalC.getSubResource(resNameStart, TimeResource.class);
		res.create();
		long prevVal = res.getValue();
		res.setValue(am.getFrameworkTime());

		//FIXME testing
		/*if(fendoUpdateTimer == null) {
			fendoUpdateTimer = new CountDownDelayedExecutionTimer(am, 5000) {
				
				@Override
				public void delayedExecution() {
					//update Fendo
					Map<Path, DataRecorderReference> dbs = fendoFactory.getAllInstances();
					logger.info("Updating "+dbs.size()+" FendoDB instances...");
					for(Entry<Path, DataRecorderReference> e: dbs.entrySet()) {
						try {
							e.getValue().getDataRecorder().reloadDays();
						} catch (IOException e1) {
							logger.warn("Could not update:", e1);
						}
					}
					fendoUpdateTimer = null;
				}
			};
		}*/

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
		String user = checkAccess(req, resp, path);
		if (user == null) {
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
		String gwId;
		String targets = target.toString();
		if(targets.startsWith(startString) && targets.length() >= (startIdx+6)) {
			gwId = targets.substring(startIdx, startIdx+6);
			//Execute this in application thread, does not work here
			new CountDownDelayedExecutionTimer(am, 1) {
				@Override
				public void delayedExecution() {
					EvalCollection evalC = EvalHelper.getEvalCollection(am);
					TimeResource res = evalC.getSubResource(resNameStart+gwId, TimeResource.class);
					res.create();
					long prevVal = res.getValue();
					res.setValue(am.getFrameworkTime());
					if((am.getFrameworkTime() - prevVal) > 10*60000) {
						for(TimeResource tr: evalC.getSubResources(TimeResource.class, false)) {
							String name = tr.getName();
							if(name.startsWith(resNameStart)) {
								logger.info("Last data received from "+name.substring(resNameStart.length())+" at "+TimeUtils.getDateAndTimeString(tr.getValue()));
							}
						}
					}
				}
			};
		}
		if(fendoUpdateTimer == null) {
			fendoUpdateTimer = new CountDownDelayedExecutionTimer(am, 15*60000) {
				
				@Override
				public void delayedExecution() {
					//update Fendo
					Map<Path, DataRecorderReference> dbs = fendoFactory.getAllInstances();
					logger.info("Updating "+dbs.size()+" FendoDB instances...");
					for(Entry<Path, DataRecorderReference> e: dbs.entrySet()) {
						try {
							e.getValue().getDataRecorder().reloadDays();
						} catch (Exception e1) {
							logger.warn("Could not update "+e.getKey().toString()+":", e1);
						}
					}
					fendoUpdateTimer = null;
				}
			};
			logger.info("Scheduled update of FendoDB instances after 15 minutes...");
		}
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
	
	
	private String checkAccess(HttpServletRequest req, HttpServletResponse resp, String resourcePath) throws ServletException, IOException {
		if (restAccess.authenticate(req, resp) == null)
			return null;
		return permMan.getAccessManager().getCurrentUser();
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
