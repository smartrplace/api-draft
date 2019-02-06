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
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ogema.core.application.ApplicationManager;

public class FileUploadUtil {
	public final static String UPLOAD_SERVER_PROPERTY = "org.smartrplace.tools.upload.servlet.url";
	public final static String UPLOAD_SERVLET_PROPERTY = "org.smartrplace.tools.upload.servlet.path";
	public final static String UPLOAD_INTERVAL_PROPERTY = "org.smartrplace.tools.upload.interval";
	public final static String UPLOAD_REMOTE_USER_PROPERTY = "org.smartrplace.tools.upload.remote.machine.user";
	public final static String UPLOAD_REMOTE_PW_PROPERTY = "org.smartrplace.tools.upload.remote.machine.user.pw";

	
	public final static String UPLOAD_SERVLET;
	public final static long UPDATE_INTERVAL;
	public final static String REMOTE_USER;
	public final static String REMOTE_PW;

	static {
		UPDATE_INTERVAL = AccessController.doPrivileged(new PrivilegedAction<Long>() {

			@Override
			public Long run() {
				return Long.getLong(UPLOAD_INTERVAL_PROPERTY, 24*3600*1000);
			}
		});
		REMOTE_USER = AccessController.doPrivileged(new PrivilegedAction<String>() {

			@Override
			public String run() {
				return System.getProperty(UPLOAD_REMOTE_USER_PROPERTY, "rest");
			}
		});
		REMOTE_PW = AccessController.doPrivileged(new PrivilegedAction<String>() {

			@Override
			public String run() {
				return System.getProperty(UPLOAD_REMOTE_PW_PROPERTY, "rest");
			}
		});
		UPLOAD_SERVLET = AccessController.doPrivileged(new PrivilegedAction<String>() {

			@Override
			public String run() {
				String server = System.getProperty(UPLOAD_SERVER_PROPERTY, "https://localhost:8444");
				if (server.endsWith("/"))
					server = server.substring(0, server.length()-1);
				String servlet = System.getProperty(UPLOAD_SERVLET_PROPERTY, "/org/smartrplace/tools/upload/servlet");
				if (!servlet.startsWith("/"))
					servlet = "/" + servlet;
				if (servlet.endsWith("/"))
					servlet = servlet.substring(0, servlet.length()-1);
				return server + servlet;
			}
		});
		
	}

	/**we assume the directory has no subdirectories!*/
	public static int sendSimpleDirectory(Path path, String remotePath, String remoteUser, String remotePw,
			String serverPortAddress, ApplicationManager am,
			SendFileUntilSuccessListener listener) {
		Collection<File> files2zip = FileUtils.listFiles(path.toFile(), null, true);
		int retval = 0;
		for(File f: files2zip) {
			SendFileUntilSuccess task = new SendFileUntilSuccess(f.toPath(), remotePath, serverPortAddress,
					remoteUser, remotePw, am, listener);
			int st = task.getStatus();
			if((st != 0)&&(st != 500)) retval++;
			//retval += upload(f.toPath(), remotePath, remoteUser, remotePw, serverPortAddress, am);
		}
		return retval;
	}
	
	/**
	 * 
	 * @param path
	 * @param remotePath
	 * @param remoteUser
	 * @param remotePw
	 * @param serverPortAddress
	 * @param am
	 * @return upload result or -1: general exception, -2: host not reachable, -3: internet not reachable
	 */
	public static int sendFile(Path path, String remotePath, String remoteUser, String remotePw, String serverPortAddress, ApplicationManager am) {
		try {
			return upload(path, remotePath, remoteUser, remotePw, serverPortAddress, am);
		} catch (HttpHostConnectException e) {
			am.getLogger().warn(e.getMessage());
			am.getLogger().debug("Exception detail:", e);
			return -2;
		} catch (UnknownHostException| NoRouteToHostException e) {
			am.getLogger().warn(e.getMessage());
			am.getLogger().debug("Exception detail:", e);
			return -3;
		} catch (KeyManagementException | NoSuchAlgorithmException | IOException e) {
			am.getLogger().warn(e.getMessage());
			am.getLogger().debug("Exception detail:", e);
			return -1;
		}
	}
	
	/** path must be relative to current working directory
	 * 
	 * @param path
	 * @param remotePath
	 * @param remoteUser
	 * @param remotePw
	 * @param serverPortAddress
	 * @param am
	 * @return
	 * @throws org.apache.http.conn.HttpHostConnectException when no connection to host is possible.
	 * @throws IOException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @return 500 is a sign that the file already exists on the server and cannot be overwritten
	 */
	public static int upload(Path path, String remotePath, String remoteUser, String remotePw, String serverPortAddress,
			ApplicationManager am) throws IOException, KeyManagementException, NoSuchAlgorithmException {
		if (remoteUser == null) remoteUser = REMOTE_USER;
		if (remotePw == null) remotePw = REMOTE_PW;
		if (serverPortAddress == null) 
			serverPortAddress = UPLOAD_SERVLET;
		if (serverPortAddress.endsWith("/"))
			serverPortAddress = serverPortAddress.substring(0, serverPortAddress.length()-1);
		Path parent = path.getParent();
		String pathInfo;
		if(remotePath == null) {
			pathInfo = (parent != null ? "/" + parent.toString().replace("\\", "/") : "");
		} else {
			pathInfo = remotePath.replace("\\", "/");
		}
		final StringBuilder address = new StringBuilder();
		address.append(serverPortAddress);
		if (!pathInfo.isEmpty() && !pathInfo.startsWith("/"))
			address.append('/');
		address.append(pathInfo)
			.append("?user=").append(remoteUser).append("&pw=").append(remotePw);
		
		HttpPost post = new HttpPost(address.toString());
		am.getLogger().debug("Sending POST to {}, file {}", address, path);
		FileBody fileBody = new FileBody(path.toFile(), ContentType.APPLICATION_OCTET_STREAM); // or DEFAULT_BINARY?
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.addPart("file", fileBody);
		HttpEntity entity = builder.build();
		//
		CloseableHttpClient client = getClient();
		post.setEntity(entity);
		HttpResponse response = client.execute(post);
		StatusLine sl = response.getStatusLine();
		int sc = sl.getStatusCode();
		if (sc < 300) {
			am.getLogger().debug("Upload response: {}", sc);
			return 0;
		}
		else {
			// FIXME do not log pw
			am.getLogger().info("Upload response: {}, reason {}, address {}", sc, sl.getReasonPhrase(), address);
			return sc;
		}
	}
	
	private static CloseableHttpClient getClient() throws KeyManagementException, NoSuchAlgorithmException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<CloseableHttpClient>() {

				@Override
				public CloseableHttpClient run() throws Exception {
					return HttpClients.custom()
							.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE) // FIXME insecure
							.setSSLContext(getSSLContext())
							.build();
				}
			});
		} catch (PrivilegedActionException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof KeyManagementException)
				throw (KeyManagementException) cause;
			if (cause instanceof NoSuchAlgorithmException)
				throw (NoSuchAlgorithmException) cause;
			throw new RuntimeException(cause);
		}
		
	}
	
	private static SSLContext getSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
		
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(km, tm, random);
		return ctx;
	}
	
	private final static SecureRandom random = new SecureRandom();
	
	private final static KeyManager[] km = new KeyManager[] {
		new X509KeyManager() {
			
			@Override
			public String[] getServerAliases(String keyType, Principal[] issuers) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public PrivateKey getPrivateKey(String alias) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String[] getClientAliases(String keyType, Principal[] issuers) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public X509Certificate[] getCertificateChain(String alias) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
				return "client";
			}
		}
			
	};
	
	private final static TrustManager[] tm = new TrustManager[] {
		new X509TrustManager() {
			
			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			
			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
			
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				throw new CertificateException("not supported");
			}
		}
	};
}
