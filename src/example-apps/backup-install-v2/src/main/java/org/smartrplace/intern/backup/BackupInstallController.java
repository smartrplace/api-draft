package org.smartrplace.intern.backup;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.gateway.remotesupervision.GatewayTransferInfo;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.intern.backup.pattern.SCPTransferPattern;
import org.smartrplace.intern.backup.patternlistener.SCPTransferListener;

import de.iwes.util.resource.ResourceHelper;

public class BackupInstallController {

	public OgemaLogger log;
    public ApplicationManager appMan;
    private ResourcePatternAccess advAcc;

	public LocalGatewayInformation gateway; // may be null
	public GatewayTransferInfo remoteSupervision; // may be null
	//public IncidentProviderSpaceBase incidentReporting;
	
    public BackupInstallController(ApplicationManager appMan) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.advAcc = appMan.getResourcePatternAccess();
		//incidentReporting = new IncidentProviderSpaceBase(appMan);
		
        reinitTransferData();
        long demandRetard = Long.getLong("org.smartrplace.backup-install.retardation", 1*30000l);
System.out.println("  WAIT FOR INIT DEMANDS with retard:"+demandRetard);				
/*if(demandRetard > 0) new CountDownDelayedExecutionTimer(appMan, demandRetard) {
@Override
public void delayedExecution() {
System.out.println("  INIT DEMANDS FOR BackupInstall V2");				
	initDemands();
}
}*/
		if(demandRetard > 0) {
			appMan.createTimer(demandRetard, new TimerListener() {
				
				@Override
				public void timerElapsed(Timer timer) {
System.out.println("  INIT DEMANDS FOR BackupInstall V2");
					timer.destroy();
					initDemands();
				}
			});
		} else {
			System.out.println("  INIT DEMANDS FOR BackupInstall V2 directly");				
			initDemands();
		}
 	}

	//public BackupActionListener backupActionListener;
	public SCPTransferListener sCPTransferListener;
//	public OGEMARundirBackupPatternListener oGEMARundirBackupPatternListener;
	// unused
//	public OGEMADatabaseBackupActionListener oGEMADatabaseBackupActionListener;
	//public GatewayTransferInfoListener gatewayTransferInfoListener;
	
//	public OGEMADatabaseBackupActionPattern databaseBackupAction = null;
    
    /**
     * @return may be null
     */
    public LocalGatewayInformation getGateway() {
		return gateway;
	}
    
    /**
     * @return may be null
     */
    public GatewayTransferInfo getRemoteSupervision() {
		return remoteSupervision;
	}

    public ApplicationManager getAppMan() {
		return appMan;
	}
    
    public void reinitTransferData() {
    	if (gateway == null)
    		this.gateway = createLocalGatewayInfo();
    	if (remoteSupervision == null) 
    		this.remoteSupervision = initTransferList();
    }
    
    
    @SuppressWarnings("unchecked")
	private final GatewayTransferInfo initTransferList() {
		ResourceList<GatewayTransferInfo> rsl = ResourceHelper.getTopLevelResource("RemoteSuperVisionList", ResourceList.class,
				appMan.getResourceAccess());
		if (gateway != null && (rsl == null || rsl.size() == 0)) {
			rsl = appMan.getResourceManagement().createResource("RemoteSuperVisionList", ResourceList.class);
			rsl.setElementType(GatewayTransferInfo.class);
			final String id =  ResourceUtils.getValidResourceName(gateway.id().getValue());
			final GatewayTransferInfo gti = rsl.addDecorator(id, GatewayTransferInfo.class);
			gti.id().<StringResource> create().setValue(id);
			rsl.activate(true);
		}
		return rsl != null && rsl.size() > 0 ? rsl.getAllElements().get(0) :  null;
    }
    
    private final LocalGatewayInformation createLocalGatewayInfo() {
    	LocalGatewayInformation gateway = appMan.getResourceAccess().getResource("OGEMA_Gateway");
	   	if (gateway == null) {
	   		String s = System.getProperty(Constants.PROPERTY_SIMULATED_ID);
	   		if (s!=null) {
	   		   	gateway = appMan.getResourceManagement().createResource("OGEMA_Gateway", LocalGatewayInformation.class);
	   	    	gateway.id().<StringResource>create().setValue(s);
	   	    	gateway.name().<StringResource>create().setValue("SmartrplaceBox");
	   	    	gateway.activate(true);	   			
	   		}
	   	} else if (Boolean.getBoolean(Constants.PROPERTY_SIMULATE_ID)) {
	   		final String id = System.getProperty(Constants.PROPERTY_SIMULATED_ID);
	   		if (id != null)
	   			gateway.id().<StringResource> create().setValue(id);
	   	}
	   	return gateway;
    }
    
    /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
		//backupActionListener = new BackupActionListener(this);
		//advAcc.addPatternDemand(BackupPattern.class, backupActionListener, AccessPriority.PRIO_LOWEST, this);
		sCPTransferListener = new SCPTransferListener(this);
		advAcc.addPatternDemand(SCPTransferPattern.class, sCPTransferListener, AccessPriority.PRIO_LOWEST, this);
//		oGEMARundirBackupPatternListener = new OGEMARundirBackupPatternListener(this);
//		advAcc.addPatternDemand(OGEMARundirBackupPattern.class, oGEMARundirBackupPatternListener, AccessPriority.PRIO_LOWEST, this);
//		oGEMADatabaseBackupActionListener = new OGEMADatabaseBackupActionListener(this);
//		advAcc.addPatternDemand(OGEMADatabaseBackupActionPattern.class, oGEMADatabaseBackupActionListener, AccessPriority.PRIO_LOWEST, this);
		//gatewayTransferInfoListener = new GatewayTransferInfoListener(this);
		//advAcc.addPatternDemand(GatewayTransferInfoPattern.class, gatewayTransferInfoListener, AccessPriority.PRIO_LOWEST);
    }

	public void close() {
		//advAcc.removePatternDemand(BackupPattern.class, backupActionListener);
		advAcc.removePatternDemand(SCPTransferPattern.class, sCPTransferListener);
//		advAcc.removePatternDemand(OGEMARundirBackupPattern.class, oGEMARundirBackupPatternListener);
//		advAcc.removePatternDemand(OGEMADatabaseBackupActionPattern.class, oGEMADatabaseBackupActionListener);
		//advAcc.removePatternDemand(GatewayTransferInfoPattern.class, gatewayTransferInfoListener);
    }

}
