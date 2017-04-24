package me.selfproject.internalserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.selfproject.constants.CommonConstants;
import me.selfproject.tools.MessageTool;

public class InternalProxyServerHandle implements Runnable {
	
	
	private String remoteServerIP;
	private int remoteServerPort;
	
	private String localServerIP;
	private int localServerPort;
	
	public InternalProxyServerHandle(String localServerIP , int localServerPort , String remoteServerIP , int remoteServerPort){
		
		this.remoteServerIP = remoteServerIP;
		this.remoteServerPort = remoteServerPort;
		this.localServerIP = localServerIP;
		this.localServerPort = localServerPort;
		
	}
	
	@Override
	public void run() {
		
		
		Socket socket = new Socket();
		Socket redirectSocket =  null;
		
		try {
			socket.connect(new InetSocketAddress(remoteServerIP, remoteServerPort));
			BufferedOutputStream  bufferOut  = new BufferedOutputStream(socket.getOutputStream());
			
			BufferedInputStream bufferIn = new BufferedInputStream(socket.getInputStream());
			
			bufferOut.write(CommonConstants.MSG_NEWCONN.getBytes());
			bufferOut.flush();
			
			while(true){
				
				// write the header
				BufferedOutputStream redirectOufferOut = null;
				
				byte[] inputBuffer = new byte[1024*10];
				int status = 0 ; 
				int responseSize =0;
				while((status = bufferIn.read(inputBuffer))!=-1){
					
					if(new String(inputBuffer).startsWith(CommonConstants.MSG_CONNCLOSE)){
						log("the client close the connection , inner socket closed . ");
						if(redirectSocket!=null){
							redirectSocket.close();
						}
					}else{
						
						if(redirectSocket==null){
							redirectSocket = new Socket();
							redirectSocket.connect(new InetSocketAddress(localServerIP, localServerPort));
							redirectOufferOut = new BufferedOutputStream(redirectSocket.getOutputStream());
							new Thread(new ResponseHandler(redirectSocket,socket,String.valueOf(Thread.currentThread().getId()))).start();
							log("request-"+Thread.currentThread().getId()+":\n");
						}
						if(status!=inputBuffer.length){
							byte[] tmpBuf = new byte[status];
							System.arraycopy(inputBuffer, 0, tmpBuf, 0, status);
							redirectOufferOut.write(tmpBuf);
							log(new String(tmpBuf));
						}else{
							redirectOufferOut.write(inputBuffer);
							log(new String(inputBuffer));
						}
						redirectOufferOut.flush();
						
					}
					responseSize+=status;
				}
				
				if(status==-1){
					
					log("proxy server close the connection");
					log("request size-"+Thread.currentThread().getId()+":\n " + responseSize + "(Bytes)");
					bufferOut.write(CommonConstants.MSG_CONNCLOSE.getBytes());
					bufferOut.flush();
					socket.close();
					redirectSocket.close();
				}
				
			}
			
		} catch (Exception e) {
			
			e.printStackTrace();
			try {
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
	}
	

	/**
	 * 
	 *Listen on the output stream 
	 *
	 */
	final class ResponseHandler implements Runnable {
		
		private Socket serviceSocket ;
		
		private Socket clientSocket;
		
		private String mainThreadID ; 
		
		public ResponseHandler(Socket serviceSocket , Socket clientSocket , String mainThreadID){
			
			this.serviceSocket = serviceSocket;
			this.clientSocket = clientSocket;
			this.mainThreadID = mainThreadID;
		}
		
		public void run() {
			
			try {
				BufferedInputStream bufferIn = new BufferedInputStream(serviceSocket.getInputStream());
				BufferedOutputStream bufferOut = new BufferedOutputStream(clientSocket.getOutputStream());
				
				//wait for the server close the connection
				byte[] outputBuffer = new byte[1024*512];
				int status = 0 ; 
				int responseSize =0;
				while((status = bufferIn.read(outputBuffer))!=-1){
					
					if(status!=outputBuffer.length){
						byte[] tmpBuf = new byte[status];
						System.arraycopy(outputBuffer, 0, tmpBuf, 0, status);
						bufferOut.write(tmpBuf);
					}else{
						bufferOut.write(outputBuffer);
					}
					responseSize+=status;
					bufferOut.flush();
					
				}
				
				if(status==-1){
					
					log("inner server close the connection");
					log("response size-"+mainThreadID+":\n " + responseSize + "(Bytes)");
					bufferOut.write(CommonConstants.MSG_CONNCLOSE.getBytes());
					bufferOut.flush();
					serviceSocket.close();
				}
				
			} catch (IOException e) {
				log("exceptions occur , close the sockets , "+e.getMessage());
				try {
					serviceSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			
		}
		
	}
	private void log(String msg){
		
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"-"+Thread.currentThread().getId()+":"+msg);
		
	}

}
