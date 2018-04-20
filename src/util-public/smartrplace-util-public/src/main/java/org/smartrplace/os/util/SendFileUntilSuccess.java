package org.smartrplace.os.util;

import java.nio.file.Path;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.gateway.remotesupervision.FileTransmissionTaskData;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;

public class SendFileUntilSuccess {
	final Path sourceToUse;
	final String dest;
	final String user;
	final String pw;
	//final int port;
	final ApplicationManager appMan;
	final String serverPortAddress;
	SendFileUntilSuccessListener listener = null;

	/**Return status of operation or:
	 * -10: object created, not configured
	 * -11: first transfer ongoing
	 * -1001..-1000+X: X transfers failed
	 * positive: finished without exception (does not mean success)
	 * -1: finished with exception
	 */
	int status = -10;
	int maxRetryNum = 10;
	long retryInterval = 60*60000;
	long initialDelay = 0;
	final TimeResource serverUnavailableUntil;
	int attemptCount = 0;

	public SendFileUntilSuccess(Path sourceToUse, String dest, String host, int port, String user, String pw,
			ApplicationManager appMan, int maxRetryNum, long retryInterval) {
		this(sourceToUse, dest, "https://"+host+":"+port+"/org/smartrplace/tools/upload/servlet",
				user, pw, appMan, maxRetryNum, retryInterval);
	}
	public SendFileUntilSuccess(Path sourceToUse, String dest, String serverPortAddress, String user, String pw,
			ApplicationManager appMan, int maxRetryNum, long retryInterval) {
		this.maxRetryNum = maxRetryNum;
		this.sourceToUse = sourceToUse;
		this.dest = dest;
		this.user = user;
		this.pw = pw;
		this.serverPortAddress = serverPortAddress;
		this.appMan = appMan;
		this.retryInterval = retryInterval;
		this.serverUnavailableUntil = null;
		init();
	}
	//TODO: Clean up constructors regarding listeners
	public SendFileUntilSuccess(Path sourceToUse, String dest, String host, int port, String user, String pw,
			FileTransmissionTaskData taskData, ApplicationManager appMan) {
		this(sourceToUse, dest, "https://"+host+":"+port+"/org/smartrplace/tools/upload/servlet",
				user, pw, taskData, appMan, null);
	}
	public SendFileUntilSuccess(Path sourceToUse, String dest, String serverPortAddress, String user, String pw,
			FileTransmissionTaskData taskData,
			ApplicationManager appMan, SendFileUntilSuccessListener listener) {
		if(taskData.maxRetry().isActive())
			this.maxRetryNum = taskData.maxRetry().getValue();
		this.listener = listener;
		this.sourceToUse = sourceToUse;
		this.dest = dest;
		this.appMan = appMan;
		this.serverPortAddress = serverPortAddress;
		this.user = user;
		this.pw = pw;
		if((taskData!= null)&& taskData.serverUnavailableUntil().isActive())
			this.serverUnavailableUntil = taskData.serverUnavailableUntil();
		else this.serverUnavailableUntil = null;
		if((taskData!= null)&& taskData.retryInterval().isActive())
			this.retryInterval = taskData.retryInterval().getValue();
		if((taskData!= null)&& taskData.initialDelay().isActive())
			this.initialDelay = taskData.initialDelay().getValue();
		init();
	}

	public int getMaxRetryNum() {
		return maxRetryNum;
	}
	public int getStatus() {
		return status;
	}
	public int getNumberOfConnectAttempts() {
		return attemptCount;
	}
	
	Timer t;
	private void init() {
		if(initialDelay > 0) new CountDownDelayedExecutionTimer(appMan, initialDelay) {
			@Override
			public void delayedExecution() {
				runInit();
			}
		};
		else runInit();
	}
	private void runInit() {
		status = -11;
		int code = performAttempt();
		if((code == -2)||(code == -3)) {
			t = appMan.createTimer(retryInterval, new TimerListener() {
				@Override
				public void timerElapsed(Timer timer) {
					peformReAttempt();
				}
			});
		}
		else if(listener != null) listener.finished(this);
	}
	
	private int performAttempt() {
		int code;
		if((serverUnavailableUntil != null) && 
				(serverUnavailableUntil.getValue() > appMan.getFrameworkTime())) {
			code = -2;
		} else {
			code = FileUploadUtil.sendFile(sourceToUse, dest, user, pw, serverPortAddress, appMan);			
			if(((code == -2)||(code == -3))&&(serverUnavailableUntil != null)) {
				serverUnavailableUntil.setValue(appMan.getFrameworkTime()+retryInterval/2);
			}
		}
		attemptCount++;
		setStatus(code);
		return code;
	}

	private void peformReAttempt() {
		int code = performAttempt();
		appMan.getLogger().info("Attempt#:"+attemptCount+"/"+maxRetryNum+" Code:+code"+" Status:"+status);
		if(((code != -2)&&(code != -3)) || (attemptCount >= maxRetryNum)) {
			t.destroy();
			if(listener != null) listener.finished(this);
			return;
		}
	}
	
	private void setStatus(int sendCode) {
		switch(sendCode) {
		case -1:
			status = -1;
			break;
		case -2:
		case -3:
			status = -1000 - attemptCount;
			break;
		default:
			status = sendCode;
		}
	}
}
