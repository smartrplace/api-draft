package org.smartrplace.os.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.ogema.core.application.ApplicationManager;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import de.iwes.util.format.StringFormatHelper;


public class SCPUtil {
	public static void createSourceInfo(Path dest, String source) {
		try {
			FileUtils.write(dest.resolve("sourceInfo.txt").toFile(), source, (String)null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static int fetchViaSCPCert(String sourcePath, Path destDir, String user, String password, String host, int port) {
		return fetchViaSCP(sourcePath, destDir, user, password, host, port, true);		
	}
	public static int fetchViaSCP(String sourcePath, Path destDir, String user, String password, String host, int port) {
		return fetchViaSCP(sourcePath, destDir, user, password, host, port, false);
	}
	/** Note that source path may contain wildcards*/
	public static int fetchViaSCP(String sourcePath, Path destDir, String user, String password, String host, int port,
			boolean pwIsCertPath) {
		FileOutputStream fos=null;

		String rfile=sourcePath;
		String lfile=destDir.toString();
		DirUtils.makeSureDirExists(destDir);	
		try {
			String prefix=null;
			if(new File(lfile).isDirectory()){
				prefix=lfile+File.separator;
			}

			JSch jsch=new JSch();
			Session session;
			if(pwIsCertPath) {
				jsch.addIdentity(password);
				session=jsch.getSession(user, host, 22);
			} else {
				session=jsch.getSession(user, host, 22);
				session.setPassword(password);
			}
			Properties config = new Properties();
			config.put("StrictHostKeyChecking","no");
			session.setConfig(config);
			session.connect();

			// exec 'scp -f rfile' remotely
			String command="scp -f "+rfile;
			Channel channel=session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out=channel.getOutputStream();
			InputStream in=channel.getInputStream();

			channel.connect();

			byte[] buf=new byte[1024];

			// send '\0'
			buf[0]=0; out.write(buf, 0, 1); out.flush();

			while(true){
				int c=checkAck(in);
				if(c!='C'){
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				long filesize=0L;
				while(true){
					if(in.read(buf, 0, 1)<0){
						// error
						break; 
					}
					if(buf[0]==' ')break;
					filesize=filesize*10L+(long)(buf[0]-'0');
				}

				String file=null;
				for(int i=0;;i++){
					in.read(buf, i, 1);
					if(buf[i]==(byte)0x0a){
						file=new String(buf, 0, i);
						break;
					}
				}

				// send '\0'
				buf[0]=0; out.write(buf, 0, 1); out.flush();

				// read a content of lfile
				fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
				int foo;
				while(true){
					if(buf.length<filesize) foo=buf.length;
					else foo=(int)filesize;
					foo=in.read(buf, 0, foo);
					if(foo<0){
						// error 
						break;
					}
					fos.write(buf, 0, foo);
					filesize-=foo;
					if(filesize==0L) break;
				}
				fos.close();
				fos=null;

				if(checkAck(in)!=0){
					throw new IllegalStateException("SCPUtil: fetch-Ex1");
				}

				// send '\0'
				buf[0]=0; out.write(buf, 0, 1); out.flush();
			}

			session.disconnect();
			return 0;
		}
		catch(Exception e){
			System.out.println(e);
			try{if(fos!=null)fos.close();}catch(Exception ee){}
			return -1;
		}		
	}

	/** IF the sourcePath is a directory we send all files, but no subdirectories*/
	public static int sendViaSCPCert(String sourcePath, String destDir, String user, String password, String host, int port,
			ApplicationManager appMan, boolean createDateDir) {
		return sendViaSCP(sourcePath, destDir, user, password, host, port, appMan, true, createDateDir);		
	}
	public static int sendViaSCP(String sourcePath, String destDir, String user, String password, String host, int port,
			ApplicationManager appMan, boolean createDateDir) {
		return sendViaSCP(sourcePath, destDir, user, password, host, port, appMan, false, createDateDir);
	}
	public static int sendViaSCP(String sourcePath, String destDir, String user, String password, String host, int port,
			ApplicationManager appMan, boolean pwIsCertPath, boolean createDateDir) {
		final class CopyFile {
			public CopyFile(String lfile, String rfile) {
				this.lfile = lfile;
				this.rfile = rfile;
			}
			public String lfile;
			public String rfile;
		}

		String lfileG=sourcePath;
		String rfileG=destDir;

		System.out.println("++++++SCP START");

		FileInputStream fis=null;
		try{
			JSch jsch=new JSch();
			Session session;
			if(pwIsCertPath) {
				jsch.addIdentity(password);
				session=jsch.getSession(user, host, 22);
			} else {
				session=jsch.getSession(user, host, 22);
				session.setPassword(password);
			}

			Properties config = new Properties();
			config.put("StrictHostKeyChecking","no");
			session.setConfig(config);
			session.connect();
			
			boolean ptimestamp = true;

			// exec 'scp -t rfile' remotely
			Path lpath = Paths.get(lfileG);
			List<CopyFile> cl = new ArrayList<>();
			if(createDateDir&&Files.isDirectory(lpath)) {
				//try to create directory for timestamp
			    String dateDir = StringFormatHelper.getCurrentDateForPath(appMan);
				Channel channelSF=session.openChannel("exec");
				((ChannelExec)channelSF).setCommand("cd "+rfileG + " && mkdir "+dateDir);
				channelSF.connect();
				channelSF.run();
				
				/*Channel channelSF=session.openChannel("sftp");
			    channelSF.connect();
			    ChannelSftp sftp=(ChannelSftp)channelSF;
			    sftp.cd(rfileG);
			    sftp.mkdir(dateDir);*/
			    rfileG = DirUtils.appendDirForLinux(rfileG, dateDir);
			    channelSF.disconnect();

			    Collection<File> fileList = FileUtils.listFiles(lpath.toFile(), null, false);
				for(File f: fileList) {
					String destFile = DirUtils.appendDirForLinux(rfileG, f.getName());
					cl.add(new CopyFile(f.getAbsolutePath(), destFile));
				}
			} else {
				cl.add(new CopyFile(lfileG, rfileG));
			}
			for(CopyFile cf: cl) {
				String command="scp " + (ptimestamp ? "-p" :"") +" -t "+cf.rfile;
				Channel channel=session.openChannel("exec");
				((ChannelExec)channel).setCommand(command);
	
				// get I/O streams for remote scp
				OutputStream out=channel.getOutputStream();
				InputStream in=channel.getInputStream();
	
				channel.connect();
	
				if(checkAck(in)!=0){
					throw new IllegalStateException("Ex1");
				}
	
				File _lfile = new File(cf.lfile);
	
				if(ptimestamp){
					command="T"+(_lfile.lastModified()/1000)+" 0";
					// The access time should be sent here,
					// but it is not accessible with JavaAPI ;-<
					command+=(" "+(_lfile.lastModified()/1000)+" 0\n");
					out.write(command.getBytes()); out.flush();
					if(checkAck(in)!=0){
						throw new IllegalStateException("Ex2");
					}
				}
	
				// send "C0644 filesize filename", where filename should not include '/'
				long filesize=_lfile.length();
				command="C0644 "+filesize+" ";
				if(cf.lfile.lastIndexOf(File.pathSeparator)>0){
					command+=cf.lfile.substring(cf.lfile.lastIndexOf(File.pathSeparator)+1);
				}
				else{
					command+=cf.lfile;
				}
				command+="\n";
				out.write(command.getBytes()); out.flush();
				if(checkAck(in)!=0){
					throw new IllegalStateException("Ex3");
				}
	
				// send a content of lfile
				fis=new FileInputStream(cf.lfile);
				byte[] buf=new byte[1024];
				while(true){
					int len=fis.read(buf, 0, buf.length);
					if(len<=0) break;
					out.write(buf, 0, len); //out.flush();
				}
				fis.close();
				fis=null;
				// send '\0'
				buf[0]=0; out.write(buf, 0, 1); out.flush();
				if(checkAck(in)!=0){
					throw new IllegalStateException("Ex4");
				}
				out.close();
	
				channel.disconnect();
			}
			session.disconnect();
			System.out.println("++++++SCP END");
			return 0;
		}
		catch(Exception e){
			System.out.println(e);
			try{if(fis!=null)fis.close();}catch(Exception ee){}
			return -1;
		}
	}

	static int checkAck(InputStream in) throws IOException{
		int b=in.read();
		// b may be 0 for success,
		//          1 for error,
		//          2 for fatal error,
		//          -1
		if(b==0) return b;
		if(b==-1) return b;

		if(b==1 || b==2){
			StringBuffer sb=new StringBuffer();
			int c;
			do {
				c=in.read();
				sb.append((char)c);
			}
			while(c!='\n');
			if(b==1){ // error
				System.out.print(sb.toString());
			}
			if(b==2){ // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}
}
