package org.smartrplace.os.util;

import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.osgi.framework.BundleContext;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.os.util.OSGiBundleUtil.BundleType;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class BundleRestartButton extends Button {
	public static final long MIN_INTERVAL_BUNDLE_RESTART = 5*TimeProcUtil.MINUTE_MILLIS;

	protected final BundleType type;
	protected final BundleContext bc;
	protected final Alert alert;
	protected long lastRealStart = -1;
	
	public static TimedJobMemoryData mdnsRestart = null;
	public static TimedJobMemoryData hmRestart = null;
	public static TimedJobMemoryData gwSyncRestart = null;
	
	public BundleRestartButton(WidgetPage<?> page, String id, BundleType type, BundleContext bc, Alert alert,
			DatapointService dpService, Integer standardInterval) {
		this(page, id, type, bc, alert, dpService, standardInterval, null);
	}
	public BundleRestartButton(WidgetPage<?> page, String id, BundleType type, BundleContext bc, Alert alert,
			DatapointService dpService, Integer standardInterval, Float interval) {
		super(page, id, "Restart "+type);
		this.type = type;
		this.bc = bc;
		this.alert = alert;
		if(alert != null)
			this.registerDependentWidget(alert);
		if(dpService != null) {
			TimedJobMemoryData data = dpService.timedJobService().registerTimedJobProvider(new TimedJobProvider() {
				
				@Override
				public String label(OgemaLocale locale) {
					return "Restart "+type;
				}
				
				@Override
				public String id() {
					return "BundleRestartButton"+type;
				}
				
				@Override
				public boolean initConfigResource(TimedJobConfig config) {
					if(standardInterval != null) {
						ValueResourceHelper.setCreate(config.alignedInterval(), standardInterval);
						ValueResourceHelper.setCreate(config.disable(), false);
					} else {
						ValueResourceHelper.setCreate(config.alignedInterval(), AbsoluteTiming.DAY);
						ValueResourceHelper.setCreate(config.disable(), true);
					}
					if(interval != null) {
						ValueResourceHelper.setCreate(config.interval(), interval);						
						ValueResourceHelper.setCreate(config.disable(), false);
					}
					ValueResourceHelper.setCreate(config.performOperationOnStartUpWithDelay(), -1);
					return true;
				}
				
				@Override
				public String getInitVersion() {
					return "A";
				}

				@Override
				public void execute(long now, TimedJobMemoryData data) {
					if(now - lastRealStart < MIN_INTERVAL_BUNDLE_RESTART) {
						System.out.println("Discarding restart as last only "+(now - lastRealStart)+" msec in past for "+type);
						return;
					}
					OSGiBundleUtil.restartBundle(type, bc);
					lastRealStart = now;
				}
				
				@Override
				public int evalJobType() {
					return 0;
				}
			});
			if(type.toString().toLowerCase().contains("mdns"))
				mdnsRestart = data;
			else if(type.toString().toLowerCase().contains("homematic"))
				hmRestart = data;
			else if(type.toString().toLowerCase().contains("mqttreplicator"))
				gwSyncRestart = data;
		}
	}

	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		if(type == BundleType.Gateway)
			OSGiBundleUtil.stopBundleById(0, bc);
		else
			OSGiBundleUtil.restartBundle(type, bc);
		if(alert != null)
			alert.showAlert("Restarted "+type, true, req);
	}
}
