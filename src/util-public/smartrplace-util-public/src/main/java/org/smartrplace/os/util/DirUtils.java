/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
	
	public static String getValidFilename(String in) {
		return getValidFilename(in, 250);
	}
	public static String getValidFilename(String in, int sizeLimit) {
		String result;
		if(in.length() > sizeLimit) {
			result = "H"+in.hashCode()+in.substring(0, 1)+in.substring(sizeLimit-1, sizeLimit);
			result = result.replaceAll("[\\\\/:*?\"<>|]", "_");
		} else 
			result = in.replaceAll("[\\\\/:*?\"<>|]", "_");
		return result;
	}
}
