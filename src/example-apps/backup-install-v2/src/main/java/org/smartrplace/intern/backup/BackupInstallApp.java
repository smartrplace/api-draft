package org.smartrplace.intern.backup;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.gateway.remotesupervision.GatewayTransferInfo;
import org.smartrplace.intern.backup.pattern.SCPTransferPattern;

/**
 * App that 
 * <ul>
 *   <li>creates a {@link LocalGatewayInformation} resource for the gateway
 *   <li>creates a {@link GatewayTransferInfo} resource for the gateway
 *   <li>listens for {@link SCPTransferPattern}s and transfers the configured file(s) to the configured server
 * </ul>
 */
@Component(specVersion = "1.2")
@Service(Application.class)
public class BackupInstallApp implements Application {
	public static final String urlPath = "org/smartrplace/external/backupinstall";

    private OgemaLogger log;
    private ApplicationManager appMan;
    private BackupInstallController controller;

    /*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();

        // 
        controller = new BackupInstallController(appMan);
     }

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (controller != null)
    		controller.close();
    	controller = null;
    	if (log != null)
    		log.info("{} stopped", getClass().getName());
    	log = null;
    	appMan = null;
    }
}
