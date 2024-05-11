package org.smartrplace.alarming.check;

import org.ogema.model.extended.alarming.AlarmGroupData;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class ReleaseOptionBase {
	public boolean blockIssue;
	public boolean setToTrash = false;
	public int blockingVal = 4; //default: waiting for onsite support, 8:customer
	
	//public FinalAnalysis finalAnalysis;
	
	/** Text for option to be displayed for users' choice */
	public String title;
	
	public String releaseComment;
	
	public static interface ReleaseAction {
		void onRelease(AlarmGroupData issue, InstallAppDevice iad);
	}
	public ReleaseAction action = null;
	public Object actionParameter = null;
}
