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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//based on http://www.oracle.com/technetwork/articles/java/compress-1565076.html
public class ZipUtil {
	static final int BUFFER = 2048;
	
	public static Collection<File> compressEntireDirectory(Path destination, Path source) {
		Collection<File> files2zip = FileUtils.listFiles(source.toFile(), null, true);
		ZipUtil.compress(destination, files2zip, source);
		return files2zip;
	}
	
	/**Compress latest files up to a source size of maxSize. Files are sorted by file name for now!*/
	public static Collection<File> compressDirectory(Path destination, Path source, long maxSize) {
		File folder = source.toFile();
	    File[] files = folder.listFiles();
	    Arrays.sort(files);
	    List<File> files2zip = new ArrayList<>();
	    long totalSize = 0;
	    for(int i=files.length-1; i>=0; i--) {
	    	totalSize += files[i].length();
	    	if(totalSize > maxSize) break;
	    	files2zip.add(files[i]);
	    }
	    ZipUtil.compress(destination, files2zip, source);
		return files2zip;
	}
	
	public static void compress(Path destination, Collection<File> inputFiles, Path topPath) {

		BufferedInputStream origin = null;
		ZipOutputStream out = null;
		try {
			String destDir = FilenameUtils.getPathNoEndSeparator(destination.toString());
			Path f = Paths.get(destDir);
			if(Files.notExists(f)) {
				Files.createDirectories(f);
			}
			FileOutputStream dest = new FileOutputStream(destination.toString());
			out = new ZipOutputStream(new BufferedOutputStream(dest));
			out.setMethod(ZipOutputStream.DEFLATED);
			byte data[] = new byte[BUFFER];
			
			// get a list of files from current directory
			//File f = new File(".");
			//String files[] = f.list();
			final Logger logger = LoggerFactory.getLogger(ZipUtil.class);
			for (File input: inputFiles) {
				logger.debug("Adding: {}",input.getPath());
				try {
					FileInputStream fi = new FileInputStream(input.getPath());
					origin = new BufferedInputStream(fi, BUFFER);
				
					ZipEntry entry;
					if(topPath == null)
						entry = new ZipEntry(input.getPath());
					else
						entry = new ZipEntry(topPath.relativize(input.toPath()).toString());
					out.putNextEntry(entry);
					int count;
					while((count = origin.read(data, 0,	BUFFER)) != -1) {
						out.write(data, 0, count);
					}
				} catch(FileNotFoundException e) {
					logger.warn("Could not find input file: {}, skipping it",input);
					//throw e;
				} finally {
					closeSmoothly(origin);
				}
			}
			
		} catch(Exception e) {
			LoggerFactory.getLogger(ZipUtil.class).error("",e);
		} finally {
			closeSmoothly(out);
		}
	}
	
	/** Find a certain file inside a zipFile and get it as InputStream. Note that that zipFile has to be closed
	 * after processing the stream !
	 * 
	 * @param endingPath the path of the file must end of this. The first entry matching this will be
	 * 		returned
	 * @param zipFilePath
	 * @return
	 */
	public interface ProcessFileInZip {
		void processFileInzip(InputStream stream) throws IOException;
	};
	public static boolean getFileInsideZip(String endingPath, File zipFilePath, ProcessFileInZip processor) {
		try {
			ZipFile zipFile = new ZipFile(zipFilePath);
		    Enumeration<? extends ZipEntry> entries = zipFile.entries();

		    while(entries.hasMoreElements()){
		        ZipEntry entry = entries.nextElement();
		        if(entry.getName().endsWith(endingPath)) {
		        	InputStream stream = zipFile.getInputStream(entry);
		        	processor.processFileInzip(stream);
		        	stream.close();
		        	zipFile.close();
		        	return true;
		        }
		    }
			zipFile.close();				
		    return false;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private static void closeSmoothly(Closeable stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (Exception ignore) {}
	}
	
}
