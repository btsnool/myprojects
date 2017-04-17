package me.selfproject.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import me.selfproject.constants.CommonConstants;

public class ProxyServerHandle implements Runnable {

	private Socket socket;
	
	private LinkedBlockingQueue<Socket> socketPool = new LinkedBlockingQueue<Socket>();
	
	
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
				try {
					socketPool.put(socket);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				OutputStream out = socket.getOutputStream();
				out.write(new String(CommonConstants.MSG_OK).getBytes());
				
			}else{
				
				Socket socket_ = socketPool.poll();
				OutputStream out = socket_.getOutputStream();
				out.write(data);
				out.flush();
				
				BufferedInputStream in = new BufferedInputStream(socket_.getInputStream());
				
				byte[] buffer_ = new byte[1024];
				int readingSize = -1;
				while((readingSize = in.read(buffer_))!=-1){
					new BufferedOutputStream(socket.getOutputStream()).write(buffer_, 0, readingSize);
				}
				socket.close();
				socketPool.put(socket_);
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
		}

	}

}
