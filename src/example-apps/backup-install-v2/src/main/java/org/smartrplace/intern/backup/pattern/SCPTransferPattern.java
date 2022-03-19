/**
 * Copyright 2009 - 2014
 *
 * Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Fraunhofer IIS
 * Fraunhofer ISE
 * Fraunhofer IWES
 *
 * All Rights reserved
 */
package org.smartrplace.intern.backup.pattern;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.action.Action;
import org.ogema.model.action.SCPDataCollectionAction;
import org.ogema.model.alignedinterval.RepeatingOperationConfiguration;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.gateway.remotesupervision.GatewayTransferInfo;
import org.ogema.util.action.ActionPattern;
import org.smartrplace.intern.backup.BackupInstallController;
import org.smartrplace.intern.backup.logic.SCPTransfer;

/**
 * A variant of a ResourcePattern, which is context sensitive. This means, that a context object
 * is injected upon creation. 
 */
public class SCPTransferPattern extends ActionPattern<SCPDataCollectionAction, BackupInstallController> { 
	public static final String APP_NAME = "backup-install";
	
	public StringResource host = model.host();
	@Existence(required=CreateMode.OPTIONAL)
	public IntegerResource port = model.port();
	public StringResource user = model.userName();

	/**Either pw or certPath must exist*/
	@Existence(required=CreateMode.OPTIONAL)
	public StringResource pw = model.password();
	@Existence(required=CreateMode.OPTIONAL)
	public StringResource certPath = model.certPath();
	
	public StringResource sourcePath = model.sourcePath();

	@Existence(required=CreateMode.OPTIONAL)
	public RepeatingOperationConfiguration operationTimes = model.destinationConfig().operationTimes();
	@Existence(required=CreateMode.OPTIONAL)
	public TimeResource operationTimeInterval = operationTimes.alignedTimeInterval().timeIntervalLength().fixedDuration();
	public StringResource destination = model.destination();
	@Existence(required=CreateMode.OPTIONAL)
	public BooleanResource pushMode = model.pushOperation();
	@Existence(required=CreateMode.OPTIONAL)
	public StringResource sourceInfo = model.sourceInfo();
	//@Existence(required=CreateMode.OPTIONAL)
	//public FileTransmissionTaskData taskData = model.fileTransmissionTaskData();
	@Existence(required=CreateMode.OPTIONAL)
	public IntegerResource controlByMaxSizeKb = model.controlByMaxSizeKb();
	
	@Existence(required=CreateMode.OPTIONAL)
	public ResourceList<Action> collectionActions = model.collectionActions();

	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public
	 */
	public SCPTransferPattern(Resource device) {
		super(device);
	}
	
	@Override
	protected String getControllingApplication() {
		return APP_NAME; 
	}
	
	@Override
	/** Create backup via entry in collectionAction, then 
	 * Zip content of folder sourcePath (dafault: data/semabox_01/extBackup/ )
	 * and depending on zipOnly property also send to server (or just perform zipping)
	 */
	public void performAction() {
		AccessController.doPrivileged(new PrivilegedAction<Void>() {
			@Override
			public Void run() {
				performActionUnprivileged();
				return null;
			}
		});
	}

	public void performActionUnprivileged() {
		boolean success;
		context.reinitTransferData();
		final LocalGatewayInformation gateway = context.getGateway();
		final GatewayTransferInfo remoteSupervision = context.getRemoteSupervision();
		final ApplicationManager appMan = context.getAppMan();
		
		appMan.getLogger().info("Preparing Backup Data configured by {}", model.getLocation());
		if(remoteSupervision != null) {
			remoteSupervision.lastBackupTransferTrial().setValue(appMan.getFrameworkTime());
			success = SCPTransfer.collectViaSCP(this, appMan, true, gateway,
					remoteSupervision.fileTransmissionTaskData(), "generalBackup", Boolean.getBoolean("org.smartrplace.intern.backup.pattern.zipOnly"));
			if(success) {
				remoteSupervision.lastBackupTransferSuccess().setValue(appMan.getFrameworkTime());
			} else {
				remoteSupervision.lastBackupTransferError().setValue(appMan.getFrameworkTime());				
			}
		} else {
			 success = SCPTransfer.collectViaSCP(this, appMan, true, gateway, null, "generalBackup", Boolean.getBoolean("org.smartrplace.intern.backup.pattern.zipOnly"));
		}
	}
	
	ResourceValueListener<IntegerResource> controlByKbListener = null;
	@Override
	public boolean accept() {
		super.accept();

		if(controlByMaxSizeKb.isActive() && (controlByKbListener == null)) {
			controlByKbListener = new ResourceValueListener<IntegerResource>() {
				@Override
				public void resourceChanged(IntegerResource resource) {
					if(resource.getValue() > 0) {
						performAction();
					}
				}
			};

			controlByMaxSizeKb.addValueListener(controlByKbListener, false);
		}
		return true;
	}
}
