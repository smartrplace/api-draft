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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.ogema.core.application.ApplicationManager;

public class ExecUtils {
	private static String checkExeOrCmd(String shortProgramName, String envPath) {
		Path p = Paths.get(envPath, shortProgramName+".cmd");
		if(!Files.exists(p)) {
			p = Paths.get(envPath, shortProgramName+".exe");
		}
		return p.toAbsolutePath().toString();
	}
	public static int executeOnShellBlocking(String cmd, ApplicationManager appMan, final int maxSeconds) {
		String[] cmdArr = {"/bin/sh","-c", cmd}; 
		//String cmd = "cmd /c mvn -version";
		//String cmd = "mvn -version";
		//ExecUtils.exceuteBlocking(cmd, appMan, 10, "M2");
		String os = System.getProperty("os.name");
		if(os.toLowerCase().contains("windows")) {
			appMan.getLogger().info("On Linux on shell: "+cmd);
			return -1;
		}
		return ExecUtils.exceuteBlocking(cmdArr, appMan, maxSeconds, false);
	}
	public static int exceuteBlocking(String cmd, ApplicationManager appMan, final int maxSeconds, String envForPath) {
		//ProcessBuilder pb = new ProcessBuilder(cmd);
		//Map<String, String> env = pb.environment();
		String envPath = System.getenv(envForPath);
		String os = System.getProperty("os.name");
		if(os.toLowerCase().contains("windows")) {
			int i = cmd.indexOf(' ');
			if(i<0) {
				cmd = checkExeOrCmd(cmd, envPath);
			} else {
				cmd = checkExeOrCmd(cmd.substring(0, i), envPath)+cmd.substring(i);
			}
		} else {
			if((envPath.endsWith("\\") || envPath.endsWith("/"))) {
				cmd = envPath+cmd;
			} else {
				cmd = envPath+File.separator+cmd;
			}
		}
		//activate to check paths
		/*for(Entry<String, String> s:env.entrySet()) {
			System.out.println(s.getKey()+" : "+s.getValue());
		}*/
		String[] strArr = new String[1];
		strArr[0] = cmd;
		return exceuteBlocking(strArr, appMan, maxSeconds, true);
	}
	/** If passSingleLine is true just the first element of cmd is passed and not an array*/
	public static int exceuteBlocking(String[] cmd, ApplicationManager appMan, final int maxSeconds, boolean
			passSingleLine) {
		final Process p;
		InputStream in;
		//InputStream error;

		//pb.redirectOutput(Redirect.INHERIT);
		//pb.redirectError(Redirect.INHERIT);
System.out.println("Executing command: "+cmd);
		try {
			if(passSingleLine) {
				p = Runtime.getRuntime().exec(cmd[0]);
			} else {
				p = Runtime.getRuntime().exec(cmd);
			}
			in = p.getInputStream();
			//error = p.getErrorStream();
			int counter = 0;
			while(p.isAlive()) {
System.out.println("In isAlive: "+cmd);
				Thread.sleep(1000);
				if(!p.isAlive()) {
					//process finished
					break;
				}
				if(counter > maxSeconds) {
					//process takes too much time
System.out.println("Destroy: "+cmd);
					p.destroy();
					break;
				}
				counter++;
			}
			byte[] bbuffer = new byte[4096];
			int readNum = 0;
			try {
			if(in.available() > 0) {
				readNum = in.read(bbuffer, 0, bbuffer.length-1);
			}
			}catch(IOException e){e.printStackTrace();}
			if(readNum >= 0) {
				bbuffer[readNum] = '\0';
			} else {
				bbuffer[0] = '\0';
			}
			appMan.getLogger().info("Finished Exec with code:"+p.exitValue()+" outStream("+readNum+") "+new String(bbuffer));	
			
			//Usually provides the same outpur
			/*if(error.available() > 0) {
				readNum = error.read(bbuffer, 0, bbuffer.length-1);
			}
			if(readNum >= 0) {
				bbuffer[readNum] = '\0';
			} else {
				bbuffer[0] = '\0';
			}
			appMan.getLogger().info("Finished MySQL-Dump with code:"+p.exitValue()+" errorStream("+readNum+") "+new String(bbuffer));	
			*/	
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return -1;
		}
		return p.exitValue();
	}
	
	public static float getFloatProperty(String propertyName, float defaultVal) {
		String prop = System.getProperty(propertyName);
		if(prop == null)
			return defaultVal;
		try {
			return Float.parseFloat(prop);
		} catch(NumberFormatException e) {
			return defaultVal;
		}
	}
}
