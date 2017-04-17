package me.selfproject.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

import me.selfproject.constants.CommonConstants;

public class ProxyServerHandle implements Runnable {

	private Socket socket;
	
	
	
	public  ProxyServerHandle(Socket socket) {
		
		this.socket = socket;
		
	}
	
	public void run() {
		
		try{
			byte[] data = null;
			
			byte[] buffer = new byte[1024];
			
			
			int dataLength = -1;
			while((dataLength = new BufferedInputStream(socket.getInputStream()).read(buffer))!=-1){
				
				if(data==null){
					data = Arrays.copyOf(buffer, dataLength);
				}else{
					byte[] tmpBuf = new byte[data.length+dataLength];
					System.arraycopy(data, 0, tmpBuf, 0, data.length);
					System.arraycopy(buffer, 0, tmpBuf, data.length, dataLength);
					data = tmpBuf;
				}
				
				buffer = new byte[1024];
			}
			
			String msg = new String(data,"UTF-8");
			if(msg.equals(CommonConstants.MSG_NEWCONN)){
				
			}else{
				
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				if(socket!=null)
					socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
