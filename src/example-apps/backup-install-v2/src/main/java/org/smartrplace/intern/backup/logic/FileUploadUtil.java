package org.smartrplace.intern.backup.logic;

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
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.ogema.tools.remote.ogema.auth.RemoteOgemaAuth;

/*
 * Adapted from https://github.com/smartrplace/api-draft
 */
public class FileUploadUtil {
	
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
	public static int sendFile(Path path, String remotePath, String serverPortAddress, ApplicationManager am, RemoteOgemaAuth auth) {
		try {
			return upload(path, remotePath, serverPortAddress, am, auth);
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
	public static int upload(Path path, String remotePath, String serverPortAddress,
			ApplicationManager am, RemoteOgemaAuth auth) throws IOException, KeyManagementException, NoSuchAlgorithmException {
		Objects.requireNonNull(serverPortAddress);
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
		address.append(pathInfo);
		
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
		final StatusLine sl;
		try (CloseableHttpResponse response = auth.execute(client, post)) {
			sl = response.getStatusLine();
		}
		final int sc = sl.getStatusCode();
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
