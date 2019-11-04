package org.smartrplace.rexometer.driver.emoncms;


import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.CompoundResourceEvent;
import org.ogema.core.resourcemanager.pattern.PatternChangeListener;
import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.smartrplace.rexometer.driver.emoncms.model.EmonCMSReadConfiguration;
import org.smartrplace.rexometer.driver.emoncms.pattern.EmonCMSConnectionPattern;
import org.smartrplace.rexometer.driver.emoncms.task.EmonCMSTask;
import org.smartrplace.rexometer.driver.emoncms.task.PullTask;

/**
 * Template OGEMA driver class
 */
@Component(specVersion = "1.2")
@Service(Application.class)
public class EmonCMSDriver implements Application, PatternListener<EmonCMSConnectionPattern>,
		PatternChangeListener<EmonCMSConnectionPattern> {

	public static final String urlPath = "/org/smartrplace/rexometer/driver/emoncms";
	public static final long MIN_STEP = 25_000L;

    private OgemaLogger log;
    private ApplicationManager appManager;
    private ResourcePatternAccess patternAccess;
    private Timer t;

    private final PriorityBlockingQueue<EmonCMSTask> tasks = new PriorityBlockingQueue<>();

    /*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appMan) {

        // Remember framework references for later.
        appManager = appMan;
        patternAccess = appManager.getResourcePatternAccess();
        log = appManager.getLogger();
        patternAccess.addPatternDemand(EmonCMSConnectionPattern.class, this, AccessPriority.PRIO_LOWEST);
        t = appManager.createTimer(1000L, new TimerListener() {
			
			@Override
			public void timerElapsed(Timer timer) {
				log.trace("Timer elapsed...");
				nextStep();
			}
		});
        log.info("{} started", getClass().getName());
   }


	/*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
        log.info("{} being stopped", getClass().getName());
        patternAccess.removePatternDemand(EmonCMSConnectionPattern.class, this);
        appManager = null;
        patternAccess = null;
        log = null;
    }

    protected void connectionAdded(EmonCMSConnectionPattern conn) {
    	// TODO
    	log.info("New EmonCMS connection added ({})", conn.model.getPath());
    	getAllFields(conn);
    }
    
    protected void connectionRemoved(EmonCMSConnectionPattern conn) {
    	log.info("EmonCMS connection removed ({})", conn.model.getPath());
    	// TODO
    }
    
    public void nextStep() {
    	t.stop();
    	tasks.forEach(task -> {
    		if (task.getNextExec() > appManager.getFrameworkTime()) {
    			return;
    		}
    		try {
				task.call();
			} catch (Exception e) {
				log.warn("EmonCMS fetch failed: {}", e.toString());
				if(log.isTraceEnabled())
					e.printStackTrace();
			} finally {
				task.setNextExec(appManager.getFrameworkTime() + task.getInterval());
				log.debug("Scheduling task #{} to run in {}ms", task.getFieldId(), task.getInterval());
			}
    	});
    	long smallestDiff = MIN_STEP * 1000;
    	long fwTime = appManager.getFrameworkTime();
    	for (EmonCMSTask task : tasks) {
    		long next = task.getNextExec();
    		long diff = next - fwTime;
    		if (diff < smallestDiff) {
    			smallestDiff = diff;
    		}
    	}
    	long diff = smallestDiff;
    	if (diff <= 0)
    		diff = 1;
    	log.debug("Next exec in {}ms", diff);
    	t.setTimingInterval(diff);
    	t.resume();
    }
    
    protected boolean getAllFields(EmonCMSConnectionPattern conn) {
    	conn.readConfiguraions.getAllElements().forEach(readConf -> {
    		log.info("Creating task for field #{} for {}", readConf.fieldId().getValue(), conn.model.getPath());
    		PullTask task = new PullTask(conn, readConf, getClient(), log);
    		tasks.add(task);
    	});
    	

    	
    	return true;
    }

	protected boolean getField(EmonCMSConnectionPattern conn, EmonCMSReadConfiguration readConf) {
		EmonCMSReader reader = new EmonCMSReader(conn, readConf, getClient(), log);
		try {
			reader.doPull();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected CloseableHttpClient getClient() {
		int to = 20000;
		RequestConfig cfg = RequestConfig.custom()
				.setConnectTimeout(to)
				.setConnectionRequestTimeout(to)
				.setSocketTimeout(to)
				.build();
		return HttpClientBuilder.create().setDefaultRequestConfig(cfg).build();
	}

	@Override
	public void patternAvailable(EmonCMSConnectionPattern pattern) {
		connectionAdded(pattern);
		appManager.getResourcePatternAccess().addPatternChangeListener(pattern, this, EmonCMSConnectionPattern.class);
	}

	@Override
	public void patternUnavailable(EmonCMSConnectionPattern pattern) {
		connectionRemoved(pattern);
		appManager.getResourcePatternAccess().removePatternChangeListener(pattern, this);
	}

	@Override
	public void patternChanged(EmonCMSConnectionPattern pattern, List<CompoundResourceEvent<?>> changes) {
		connectionRemoved(pattern);
		connectionAdded(pattern);
	}
	
}
