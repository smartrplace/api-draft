package org.smartrplace.intern.backup.logic;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.ogema.core.application.ApplicationManager;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.gateway.remotesupervision.FileTransmissionTaskData;
import org.ogema.tools.remote.ogema.auth.RemoteOgemaAuth;
import org.ogema.util.action.ActionHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.smartrplace.intern.backup.pattern.SCPTransferPattern;
import org.smartrplace.os.util.DirUtils;
import org.smartrplace.os.util.SCPUtil;
import org.smartrplace.os.util.SendFileUntilSuccess;
import org.smartrplace.os.util.ZipUtil;
import org.smartrplace.util.file.ApacheFileAdditions;

import de.iwes.util.format.StringFormatHelper;

public class SCPTransfer {
	/**
	 * 
	 * @param scp
	 * @param appMan
	 * @param createDateDir
	 * @param gateway
	 *            may be null if not gateway info needs to be inserted
	 */
	public static boolean collectViaSCP(SCPTransferPattern scp, ApplicationManager appMan, boolean createDateDir,
			LocalGatewayInformation gateway, FileTransmissionTaskData externalTransmissionControl,
			String destinationExtenstion) {
		if (appMan.getLogger().isDebugEnabled()) {
			if(scp.collectionActions.size() > 0)
				appMan.getLogger().debug("Perform action #"+scp.collectionActions.size()+" [1]:"+scp.collectionActions.getAllElements().get(0).getLocation());
			else
				appMan.getLogger().debug("No actions found in "+scp.collectionActions.getLocation());
		}
		ActionHelper.performActionListBlocking(scp.collectionActions, 20000);
		if (scp.pushMode.exists() && scp.pushMode.getValue()) {
			return sendViaSCP(scp, appMan, createDateDir, gateway, externalTransmissionControl, destinationExtenstion);
		}
		Path dest = Paths.get(scp.destination.getValue());
		DirUtils.makeSureDirExists(dest);
		Collection<File> orgFiles = FileUtils.listFiles(dest.toFile(), TrueFileFilter.TRUE, null);
		int port = 22;
		if (scp.port.isActive()) {
			port = scp.port.getValue();
		}
		if (scp.certPath.isActive()) {
			SCPUtil.fetchViaSCPCert(scp.sourcePath.getValue(), DirUtils.resolvePath(scp.destination.getValue()),
					scp.user.getValue(), scp.pw.getValue(), scp.host.getValue(), port);
		} else if (scp.pw.isActive()) {
			SCPUtil.fetchViaSCP(scp.sourcePath.getValue(), DirUtils.resolvePath(scp.destination.getValue()),
					scp.user.getValue(), scp.pw.getValue(), scp.host.getValue(), port);
		} else {
			throw new IllegalArgumentException("Either certificate or password has to be provided!");
		}
		SCPUtil.createSourceInfo(dest, scp.host.getValue() + ":" + port);

		// check if new file found and send email notification in the future
		Collection<File> newFiles = FileUtils.listFiles(dest.toFile(), TrueFileFilter.TRUE, null);
		newFiles.removeAll(orgFiles);
		BigInteger size = new BigInteger("0");
		File example = null;
		for (File f : newFiles) {
			example = f;
			size.add(BigInteger.valueOf(f.length()));
		}
		appMan.getLogger()
				.info("Received via SCP " + newFiles.size() + " files with total size "
						+ ApacheFileAdditions.byteCountToDisplaySize(size)
						+ ((newFiles.size() > 0) ? " Example:" + example.getName() : ""));
		return true;
	}

	/**
	 * For now we always zip directories, single files are transferred as they are
	 * 
	 * @param gateway
	 *            may be null if not destination replacement is required
	 * @param externalTransmissionControl
	 *            may be null
	 */
	public static boolean sendViaSCP(SCPTransferPattern scp, ApplicationManager appMan, boolean createDateDir,
			LocalGatewayInformation gateway, FileTransmissionTaskData externalTransmissionControl,
			String destinationExtenstion) {
		String dest = scp.destination.getValue();
		if (dest.equals("///GATEWAY_ID///")) {
			if (gateway == null)
				dest = "/NO_GW_INFO/";
			else
				dest = "/" + gateway.id().getValue() + "/";
		}
		// String dest = scp.destination.getValue() +"/"+
		// StringFormatHelper.getCurrentDateForPath(appMan);
		String source = DirUtils.resolvePath(scp.sourcePath.getValue()).toString();
		int port = 22;
		if (scp.port.isActive()) {
			port = scp.port.getValue();
		}
		Path sourcePath = Paths.get(source);
		String sourceInfo = scp.sourceInfo.getValue();
		if (sourceInfo.equals("///GATEWAY_ID///")) {
			if (gateway == null)
				sourceInfo = "/NO_GW_INFO/";
			else
				sourceInfo = "/" + gateway.id().getValue() + "/";
		}
		if (Files.isDirectory(sourcePath))
			SCPUtil.createSourceInfo(sourcePath, sourceInfo);

		int errorCode;
		Collection<File> sentFiles = null;
		Path sourceToUse;
		if (scp.certPath.isActive()) {
			sourceToUse = Paths.get(source);
			appMan.getLogger().debug("Sending ViaSCPCert from dir "+source + " to "+ scp.host.getValue() +"-"+ dest);
			errorCode = SCPUtil.sendViaSCPCert(source, dest, scp.user.getValue(), scp.certPath.getValue(),
					scp.host.getValue(), port, appMan, createDateDir);
//		} else if (scp.pw.isActive()) {
//			appMan.getLogger().debug("Sending with pw from dir "+source + " to "+ scp.host.getValue() +"-"+ dest);
//			errorCode = SCPUtil.sendViaSCP(scp.sourcePath.getValue(), dest, scp.user.getValue(), scp.pw.getValue(),
//					scp.host.getValue(), port, appMan, createDateDir);
		} else {
			if (Files.isDirectory(sourcePath)) {
				Path zipDestPath;
				if(destinationExtenstion != null) {
					zipDestPath = sourcePath.getParent()
							.resolve(destinationExtenstion + StringFormatHelper.getCurrentDateForPath(appMan) + ".zip");					
				} else {
					zipDestPath = sourcePath.getParent()
						.resolve("unknownData" + StringFormatHelper.getCurrentDateForPath(appMan) + ".zip");
				}
				if (scp.controlByMaxSizeKb.isActive() && (scp.controlByMaxSizeKb.getValue() > 0)) {
					sentFiles = ZipUtil.compressDirectory(zipDestPath, sourcePath, scp.controlByMaxSizeKb.getValue() * 1024);
				} else {
					sentFiles = ZipUtil.compressEntireDirectory(zipDestPath, sourcePath);
				}
				sourceToUse = zipDestPath;
			} else {
				sourceToUse = Paths.get(scp.sourcePath.getValue());
			}
			FileTransmissionTaskData taskData;
			//if (scp.taskData.isActive())
			//	taskData = scp.taskData;
			//else
			if (externalTransmissionControl != null)
				taskData = externalTransmissionControl;
			else
				taskData = null;
			if ((taskData != null) && taskData.maxRetry().isActive() && taskData.maxRetry().getValue() > 1) {
				appMan.getLogger().debug("Sending with retry from dir "+sourceToUse.toString()+ " to "+ scp.host.getValue() +"-"+ dest);
				SendFileUntilSuccess sendTask = new SendFileUntilSuccess(sourceToUse, dest, scp.host.getValue(), port,
						scp.user.getValue(), scp.pw.isActive() ? scp.pw.getValue() : "rest", appMan);
				errorCode = sendTask.getStatus();
			} else {
				final BundleContext ctx = appMan.getAppID().getBundle().getBundleContext(); // no privileged block because we are already inside one
				final ServiceReference<RemoteOgemaAuth> authRef = getAuthService(ctx, scp.host.getValue(), scp.port.getValue());
				final RemoteOgemaAuth auth = authRef != null ? ctx.getService(authRef) : null;
				if (auth == null) {
					appMan.getLogger().warn("No authentication service configured for {}:{}, cannot transfer files", scp.host.getValue(), scp.port.getValue());
					return false;
				}
				appMan.getLogger().debug("Sending direct from dir "+sourceToUse.toString()+ " to "+ scp.host.getValue() +"-"+ dest);
				try {
					errorCode = FileUploadUtil.sendFile(sourceToUse, dest, 
						"https://" + scp.host.getValue() + ":" + port + "/org/smartrplace/tools/upload/servlet",
						appMan, auth);
				} finally {
					try {
						ctx.ungetService(authRef);
					} catch (Exception ignore) {}
				}
				/*String serverPortAddress = SendFileUntilSuccess.getServerPortAdress(scp.host.getValue(),
			   			port);
				dest = scp.destination.getValue();
				if (dest.equals("///GATEWAY_ID///")) {
					if (gateway == null)
						dest = "/NO_GW_INFO";
					else
						dest = "/" + gateway.id().getValue();
				}
				System.out.println("Sending "+sourceToUse+" to "+(serverPortAddress+dest));
				errorCode = FileUploadUtil.sendFile(sourceToUse, "", scp.user.getValue(), "rest",
						serverPortAddress+dest,
						appMan);*/
				if ((errorCode == -2) || (errorCode == -3)) {
					errorCode = 999999;
				}
			}
		}

		// check if new file found and send email notification in the future
		String logTail = "<no directory>";
		if (Files.isDirectory(sourcePath)) {
			if(sentFiles == null) {
				appMan.getLogger().warn("sent files was not provided by Zip Util! {}", sentFiles);
				sentFiles = FileUtils.listFiles(new File(source), TrueFileFilter.TRUE, null);
			}
			BigInteger size = new BigInteger("0");
			File example = null;
			for (File f : sentFiles) {
				example = f;
				long locSize = f.length();
				size = size.add(BigInteger.valueOf(locSize));
			}
			logTail = sentFiles.size() + " files with total size " + ApacheFileAdditions.byteCountToDisplaySize(size)
					+ ((sentFiles.size() > 0) ? " Example:" + example.getName() : "");
		}
		boolean fail = true;
		if ((errorCode > 0) || (errorCode == -1)) {
			appMan.getLogger().info("Failed permanently while sending via SCP {} errorCode: {}. Source {}, destinstation {}", 
					logTail,errorCode, sourceToUse, dest);
		} else if (errorCode < 0) {
			appMan.getLogger().info("Failed intially while sending via SCP " + logTail + " errorCode:" + errorCode);
		} else {
			fail = false;
			appMan.getLogger().info("Sent via SCP " + logTail);
		}
		return(!fail);
	}
	
    
    private static ServiceReference<RemoteOgemaAuth> getAuthService(final BundleContext ctx, final String host, final int port) {
    	final Collection<ServiceReference<RemoteOgemaAuth>> services;
		try {
			services = ctx.getServiceReferences(RemoteOgemaAuth.class, 
					"(&(" + RemoteOgemaAuth.REMOTE_HOST_PROPERTY + "=" + host + ")(" + RemoteOgemaAuth.REMOTE_PORT_PROPERTY + "=" + port + "))");
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
    	return services != null && !services.isEmpty() ? services.iterator().next() : null;
    }
	
}
