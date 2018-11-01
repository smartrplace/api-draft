package org.smartrplace.tools.upload.server;

public class Constants {
	
	public final static String BASE_FOLDER_PROPERTY = "org.smartrplace.tools.upload.folder";
	public final static String MAX_UPLOAD_FOLDER_SIZE_PROPERTY = "org.smartrplace.tools.upload.max.folder.size";
	public final static String WARNING_UPLOAD_FOLDER_SIZE_PROPERTY = "org.smartrplace.tools.upload.warning.folder.size";
	
	public final static String BASE_FOLDER = System.getProperty(BASE_FOLDER_PROPERTY, "data/uploads");
	public final static long MAX_UPLOAD_FOLDER_SIZE = Long.getLong(MAX_UPLOAD_FOLDER_SIZE_PROPERTY, 1024 * 1024 * 1024); // 1GB default
	public final static long WARNING_UPLOAD_FOLDER_SIZE = Long.getLong(WARNING_UPLOAD_FOLDER_SIZE_PROPERTY, 1024 * 1024 * 100); // 100MB default
	
}
