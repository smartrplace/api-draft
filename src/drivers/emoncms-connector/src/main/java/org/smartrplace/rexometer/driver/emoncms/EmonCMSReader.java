package org.smartrplace.rexometer.driver.emoncms;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.rexometer.driver.emoncms.model.EmonCMSReadConfiguration;
import org.smartrplace.rexometer.driver.emoncms.pattern.EmonCMSConnectionPattern;

public class EmonCMSReader {
	
	protected final EmonCMSConnectionPattern conn;
	protected final EmonCMSReadConfiguration readConf;
	protected final CloseableHttpClient client;
	protected final OgemaLogger log;

	public EmonCMSReader(EmonCMSConnectionPattern conn, EmonCMSReadConfiguration readConf, 
			CloseableHttpClient client) {
		this(conn, readConf, client, null);
	}
	
	public EmonCMSReader(EmonCMSConnectionPattern conn, EmonCMSReadConfiguration readConf,
			CloseableHttpClient client, OgemaLogger log) {
		this.conn = conn;
		this.client = client;
		this.readConf = readConf;
		this.log = log;
	}

	public boolean doPull() throws Exception {

		if(Boolean.getBoolean("org.smartrplace.rexometer.driver.emoncms.testwithoutconnection"))
			return true;
		String base = conn.url.getValue().replaceAll("/$", "");
		String endpoint = "/timevalue.json";
		int id = readConf.fieldId().getValue();
		String uri = base + endpoint + "?id=" + id;

		HttpGet req = new HttpGet(uri);
		req.setHeader("Authorization", "Bearer " + conn.apiKeyRead.getValue());
		log.info("Fetching EmonCMS feed #{}", id);
		CloseableHttpResponse resp = client.execute(req);
		
		try {
			int code = resp.getStatusLine().getStatusCode();
			if (code != 200)
				throw new RuntimeException("Got status code " + code);

			JSONObject o;
			o = new JSONObject(new JSONTokener(resp.getEntity().getContent()));
			long time = o.getLong("time") * 1000;
			
			if (readConf.lastValueTimestamp().exists() && readConf.lastValueTimestamp().getValue() >= time) {
				if (log != null)
					log.trace("{} already has newest value", readConf.getPath());
				return true;
			}
			
			double value = o.getDouble("value");
			
			if (!readConf.destination().exists())
				readConf.destination().create();
			if (!readConf.lastValueTimestamp().exists())
				readConf.lastValueTimestamp().create();

			/*if (readConf.destination() instanceof EnergyResource) {
				((EnergyResource) readConf.destination()).setKWhs((float) value);
				readConf.lastValueTimestamp().setValue(time);
			} else */ if (readConf.destination() instanceof FloatResource) {
				((FloatResource) readConf.destination()).setValue((float) value);
				readConf.lastValueTimestamp().setValue(time);
			} else {
				if (log != null)
					log.info("Destination {} is not a FloatResource!", readConf.destination().getPath());
				return false;
			}
			
			readConf.activate(true);
			return true;
		} finally {
			resp.close();
		}
	}

}
