package org.smartrplace.os.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class DirUtils {
	/**return all files, but no directories - use FileUtils instead*/
	@Deprecated
	public static List<Path> getAllFilesFromDirectory(Path dir, boolean recursive, String globPattern) {
        //String files[] = dir.toFile().list();
        PathMatcher matcher =
        	    FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
        if(matcher.matches(dir));
		return null;
	}
	
	public static void makeSureDirExists(Path dir) {
		if(Files.notExists(dir)) {
			try {
				Files.createDirectories(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
	public static String appendPath(String orgDir, String addPath) {
		return (orgDir.endsWith("/")||orgDir.endsWith("\\"))?(orgDir+addPath):(orgDir+File.separator+addPath);
	}

	public static String appendDirForLinux(String orgDir, String addPath) {
		return (orgDir.endsWith("/"))?(orgDir+addPath):(orgDir+"/"+addPath);
	}

	public static Path resolvePath(String path) {
		if(path.startsWith("$home")) {
			return Paths.get(FileUtils.getUserDirectoryPath(), path.substring(6));
		} else {
			return Paths.get(path);
		}
	}
}
