package org.smartrplace.rexometer.driver.emoncms.task;

import org.apache.http.impl.client.CloseableHttpClient;
import org.ogema.core.logging.OgemaLogger;
import org.smartrplace.rexometer.driver.emoncms.EmonCMSReader;
import org.smartrplace.rexometer.driver.emoncms.model.EmonCMSReadConfiguration;
import org.smartrplace.rexometer.driver.emoncms.pattern.EmonCMSConnectionPattern;

public class PullTask extends EmonCMSTask {
	
	private final EmonCMSConnectionPattern conn;
	private final EmonCMSReadConfiguration readConf;
	private final CloseableHttpClient client;
	private final OgemaLogger log;

	public PullTask(EmonCMSConnectionPattern conn, EmonCMSReadConfiguration readConf,
			CloseableHttpClient client, OgemaLogger log) {
		super();
		this.conn = conn;
		this.readConf = readConf;
		this.client = client;
		this.log = log;
	}
	
	@Override
	public int getFieldId() {
		return readConf.fieldId().getValue();
	}
	
	@Override
	public long getInterval() {
		return readConf.pollRate().getValue();
	}
	
	@Override
	public EmonCMSTask call() throws Exception {
		EmonCMSReader r = new EmonCMSReader(conn, readConf, client, log);
		r.doPull();
		return this;
	}

}
