package me.selfproject.internalserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.selfproject.constants.CommonConstants;
import me.selfproject.tools.MessageTool;

public class InternalProxyServerHandle implements Runnable {
	
	
	private String remoteServerIP;
	private int remoteServerPort;
	
	public InternalProxyServerHandle(String remoteServerIP , int remoteServerPort){
		
		this.remoteServerIP = remoteServerIP;
		this.remoteServerPort = remoteServerPort;
		
	}
	@Override
	public void run() {
		Socket socket = new Socket();
		
		try {
			socket.connect(new InetSocketAddress(remoteServerIP, remoteServerPort));
			BufferedOutputStream  bufferOut  = new BufferedOutputStream(socket.getOutputStream());
			
			BufferedInputStream bufferIn = new BufferedInputStream(socket.getInputStream());
			
			bufferOut.write(CommonConstants.MSG_NEWCONN.getBytes());
			bufferOut.write(CommonConstants.MSG_SPLIT.getBytes());
			bufferOut.flush();
			
			while(true){
				
				byte[] data = MessageTool.readData(bufferIn);
				
				
				log("request:\n"+new String(data));
				
				if((CommonConstants.MSG_OK+CommonConstants.MSG_SPLIT).equals(new String(data))){
					
					
				}else{
					
					Socket redirectSocket = new Socket();
					
					redirectSocket.connect(new InetSocketAddress("183.134.5.17", 9080));
					
					BufferedOutputStream redirectOufferOut = new BufferedOutputStream(redirectSocket.getOutputStream());
					redirectOufferOut.write(data);
					redirectOufferOut.flush();
					
					BufferedInputStream redirectBufferIn_ = new BufferedInputStream(redirectSocket.getInputStream());
					
					byte[] response = MessageTool.readData(redirectBufferIn_);
					
					log("response :\n " + response);

					bufferOut.write(response);
					bufferOut.flush();
					redirectSocket.close();
					
				}
				
				
			}
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void log(String msg){
		
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"-"+msg);
		
	}

}
