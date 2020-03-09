package org.smartrplace.intern.backup.logic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.ogema.core.application.ApplicationManager;
import org.smartrplace.intern.backup.pattern.BackupPattern;
import org.smartrplace.os.util.DirUtils;
import org.smartrplace.os.util.ExecUtils;
import org.smartrplace.os.util.ZipUtil;

public class XWiki {
	public static void backupXWiki(BackupPattern bp, ApplicationManager appMan) {
		Path dest = DirUtils.resolvePath(bp.destination.getValue());
		try {
		if(FilenameUtils.getName(dest.toString()).equals("xwiki") && Files.exists(dest)) {
			FileUtils.cleanDirectory(dest.toFile());
		}
		FileUtils.copyFileToDirectory(new File("/etc/xwiki/xwiki.properties"), dest.toFile());
		FileUtils.copyFileToDirectory(new File("/etc/xwiki/xwiki.cfg"), dest.toFile());
		FileUtils.copyFileToDirectory(new File("/etc/xwiki/hibernate.cfg.xml"), dest.toFile());
		FileUtils.copyFileToDirectory(new File("/etc/xwiki/classes/logback.xml"), dest.resolve("classes").toFile());
		FileUtils.copyDirectoryToDirectory(new File("/etc/xwiki/observation"), dest.toFile());
		} catch (IOException e) {e.printStackTrace();}
		
		String cmd = "mysqldump --add-drop-database -u xwiki -p"+
				bp.xwikiPw.getValue()+" --databases xwiki > "+dest.toAbsolutePath().toString()+"/xwiki.sql";
		String[] cmdArr = {"/bin/sh","-c", cmd}; 
		//String cmd = "cmd /c mvn -version";
		//String cmd = "mvn -version";
		//ExecUtils.exceuteBlocking(cmd, appMan, 10, "M2");
		ExecUtils.exceuteBlocking(cmdArr, appMan, 30, false);

		if(bp.doZip.getValue()) {
			ZipUtil.compressEntireDirectory(dest.getParent().resolve("xwikiBackup.zip"), dest);
		}
	}
}
