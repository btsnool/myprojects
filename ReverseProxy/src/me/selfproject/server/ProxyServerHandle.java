package me.selfproject.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.selfproject.constants.CommonConstants;
import me.selfproject.tools.MessageTool;

public class ProxyServerHandle implements Runnable {

	private Socket socket;
	
	private LinkedBlockingQueue<Socket> socketPool ;
	
	
	public  ProxyServerHandle(Socket socket,LinkedBlockingQueue<Socket> socketPool) {
		
		this.socket = socket;
		
		this.socketPool = socketPool;
		
	}
	
	
	public void run() {
		
		try{
			System.out.println("new socket connection: threadid-"+Thread.currentThread().getId()+";"+socket);
			BufferedInputStream bufferIn = new BufferedInputStream(socket.getInputStream());
			
			//read request data from the client
			Socket redirectSocket =  null;
			BufferedOutputStream redirectOufferOut = null;
			byte[] inputBuffer = new byte[1024*10];
			int status = 0 ; 
			
			//listen and read the input data
			while(!socket.isClosed()&&(status = bufferIn.read(inputBuffer))!=-1){
				
				if(new String(inputBuffer).startsWith(CommonConstants.MSG_NEWCONN)){
					log("inner proxy server init the connection..");
					try {
						socketPool.put(socket);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}else if(new String(inputBuffer).startsWith(CommonConstants.MSG_CONNCLOSE)){
					log("the application server close the connection..");
					if(redirectSocket!=null){
						socket.close();
						socketPool.put(redirectSocket);
					}
					return;
				}else{
					//recevie the request from the client 
					if(redirectSocket==null){
						redirectSocket = socketPool.take();
						redirectOufferOut = new BufferedOutputStream(redirectSocket.getOutputStream());
						new Thread(new ResponseHandler(redirectSocket, socket, String.valueOf(Thread.currentThread().getId()))).start();
					}
					//write client request to the inner server
					if(status!=inputBuffer.length){
						byte[] tmpBuf = new byte[status];
						System.arraycopy(inputBuffer, 0, tmpBuf, 0, status);
						redirectOufferOut.write(tmpBuf);
						log("request-"+Thread.currentThread().getId()+":"+new String(tmpBuf));
					}else{
						redirectOufferOut.write(inputBuffer);
						log("request-"+Thread.currentThread().getId()+":"+new String(inputBuffer));
					}
					redirectOufferOut.flush();
				}
				
			}
			
					

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				if(socket!=null)
					socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return ;
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
					
					if(new String(outputBuffer).startsWith(CommonConstants.MSG_CONNCLOSE)){
						log("application server close the connection");
						clientSocket.close();
						try {
							socketPool.put(serviceSocket);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return;
					}else{
						if(status!=outputBuffer.length){
							byte[] tmpBuf = new byte[status];
							System.arraycopy(outputBuffer, 0, tmpBuf, 0, status);
							bufferOut.write(tmpBuf);
						}else{
							bufferOut.write(outputBuffer);
						}
						responseSize+=status;
					}
					bufferOut.flush();
					
				}
				
				if(status==-1){
					log("inner proxy server close the connection");
					log("response size-"+mainThreadID+":\n " + responseSize + "(Bytes)");
					serviceSocket.close();
				}
				
			} catch (IOException e) {
				log("exceptions occur , close the sockets , "+e.getMessage());
				try {
					clientSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			
		}
		
	}
	
	private void log(String msg){
		
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"-"+msg);
		
	}

}
