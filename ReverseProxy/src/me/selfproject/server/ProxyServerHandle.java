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
			BufferedOutputStream bufferOut = new BufferedOutputStream(socket.getOutputStream());
			
			//the socket never active closed
					
			byte[] msgData = MessageTool.readData(bufferIn);
			String msg = new String(msgData);
			
			log("request:\n"+msg);
			if((msg).equals(CommonConstants.MSG_NEWCONN+CommonConstants.MSG_SPLIT)){
				try {
					socketPool.put(socket);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bufferOut.write(new String(CommonConstants.MSG_OK).getBytes());
				bufferOut.write(CommonConstants.MSG_SPLIT.getBytes());
				bufferOut.flush();
			}else{
				
				Socket socket_ = socketPool.take();
				if(socket_==null){
					
					bufferOut.write("proxy error\r\n".getBytes());
					socket.close();
					
				}else{
					log("redirect request to Socket: "+socket_.toString());
					OutputStream out = socket_.getOutputStream();
					out.write(msg.getBytes(Charset.forName("UTF-8")));
//						out.write(CommonConstants.MSG_SPLIT.getBytes());
					out.flush();
					
					BufferedInputStream in = new BufferedInputStream(socket_.getInputStream());
					
					byte[]  responseData  = MessageTool.readData(in);
					log("respone:\n"+responseData);
					
					bufferOut.write(responseData);
					bufferOut.flush();
					socket.close();
					socketPool.put(socket_);
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
	
	private void log(String msg){
		
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"-"+msg);
		
	}

}
