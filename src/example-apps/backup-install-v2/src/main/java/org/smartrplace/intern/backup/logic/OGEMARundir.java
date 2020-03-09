package org.smartrplace.intern.backup.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.smartrplace.intern.backup.pattern.OGEMARundirBackupPattern;
import org.smartrplace.os.util.DirUtils;
import org.smartrplace.os.util.ZipUtil;

public class OGEMARundir {
	public static void backupRundir(final OGEMARundirBackupPattern bp, ApplicationManager appMan) {
		final Path dest = DirUtils.resolvePath(bp.destination.getValue());
		try {
		if(FilenameUtils.getName(dest.toString()).equals("ogRundir") && Files.exists(dest)) {
			FileUtils.cleanDirectory(dest.toFile());
		}
		
		//get backup into dest.toAbsolutePath().toString()+"/ogRundir"
		final ResourceValueListener<BooleanResource> listener = new ResourceValueListener<BooleanResource>() {
			@Override
			public void resourceChanged(BooleanResource arg0) {
				if(!arg0.getValue()) {
					if(bp.doZip.getValue()) {
						ZipUtil.compressEntireDirectory(dest.getParent().resolve("ogRundirBackup.zip"), dest);
					}
					arg0.removeValueListener(this);
				}
			}
		};
		bp.resAdminBackupAction.addValueListener(listener);
		bp.resAdminBackupAction.stateControl().setValue(true);
		} catch (IOException e) {e.printStackTrace();}
	}
}
