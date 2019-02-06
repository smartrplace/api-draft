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
package org.smartrplace.tools.uploadservlet2.server;

public class Constants {
	
	public final static String BASE_FOLDER_PROPERTY = "org.smartrplace.tools.upload.folder";
	public final static String MAX_UPLOAD_FOLDER_SIZE_PROPERTY = "org.smartrplace.tools.upload.max.folder.size";
	public final static String WARNING_UPLOAD_FOLDER_SIZE_PROPERTY = "org.smartrplace.tools.upload.warning.folder.size";
	
	public final static String BASE_FOLDER = System.getProperty(BASE_FOLDER_PROPERTY, "data/uploads");
	public final static long MAX_UPLOAD_FOLDER_SIZE = Long.getLong(MAX_UPLOAD_FOLDER_SIZE_PROPERTY, 1024 * 1024 * 1024); // 1GB default
	public final static long WARNING_UPLOAD_FOLDER_SIZE = Long.getLong(WARNING_UPLOAD_FOLDER_SIZE_PROPERTY, 1024 * 1024 * 100); // 100MB default
	
}
