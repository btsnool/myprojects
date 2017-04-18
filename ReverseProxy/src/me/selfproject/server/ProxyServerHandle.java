package me.selfproject.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.selfproject.constants.CommonConstants;

public class ProxyServerHandle implements Runnable {

	private Socket socket;
	
	private LinkedBlockingQueue<Socket> socketPool ;
	
	
	public  ProxyServerHandle(Socket socket,LinkedBlockingQueue<Socket> socketPool) {
		
		this.socket = socket;
		
		this.socketPool = socketPool;
		
	}
	
	private boolean checkIsStreamEnd(byte[] by){
		
		byte[] tmpBuf = Arrays.copyOfRange(by, by.length-CommonConstants.MSG_SPLIT.length(), by.length);
		
		try {
			if(CommonConstants.MSG_SPLIT.equals(new String(tmpBuf,"UTF-8"))){
				return true;
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private boolean checkContentLength(byte[] content){
		
		try {
			String tmpString = new String(content,"UTF-8");
			
			System.out.println("content----"+tmpString);
			
			Pattern pattern = Pattern.compile("(?<=Content-Length: )\\d+");
			
			Matcher ma = pattern.matcher(tmpString);
			
			if(ma.find()){
				
				int contentLength = Integer.valueOf(ma.group());
				
				Pattern messageBodyReg = Pattern.compile("(?<=\r\n\r\n)(.*[\r\n]{1,2})*");
				
				Matcher ma_ = messageBodyReg.matcher(tmpString);
				
				if(ma_.find()){
					
					String messageBody = ma_.group();
//					System.out.println(messageBody);
//					System.out.println("content-length : " + contentLength+";actual size:"+messageBody.length());
					if(messageBody.length() == content.length){
						return true;
					}
				}
			
			}else{
				if(tmpString.endsWith("\r\n\r\n")){
					return true;
				}
			}
			
		} catch (UnsupportedEncodingException e) {
			
		}
		return false;
		
		
	}
	private String reaData(InputStream in) throws Exception{
		
		
		byte[] data = null;
		
		boolean endOfRequest = false;
		while(!endOfRequest){
			
			byte[] buffer = new byte[1024];
			int dataLength = in.read(buffer);
			if(dataLength!=-1){
				//read data
				if(data==null){
					data = Arrays.copyOf(buffer, dataLength);
				}else{
					byte[] tmpBuf = new byte[data.length+dataLength];
					System.arraycopy(data, 0, tmpBuf, 0, data.length);
					System.arraycopy(buffer, 0, tmpBuf, data.length, dataLength);
					data = tmpBuf;
				}
				//detect the end of request
				if(checkIsStreamEnd(data)){
					endOfRequest = true;
				}
			}else{
				endOfRequest = true;
			}
			if(data==null){
				endOfRequest = false;
			}else{
				if(checkContentLength(data)){
					endOfRequest = true;
				}
			}
			
			
		}
		return new String(data,"UTF-8");
	}
	
	public void run() {
		
		try{
			System.out.println("new socket connection...");
			BufferedInputStream bufferIn = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream bufferOut = new BufferedOutputStream(socket.getOutputStream());
			
			//the socket never active closed
			while(true){
					
				String msg = reaData(bufferIn);
//				System.out.println("request msg:"+msg);
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
						OutputStream out = socket_.getOutputStream();
						out.write(msg.getBytes(Charset.forName("UTF-8")));
//						out.write(CommonConstants.MSG_SPLIT.getBytes());
						out.flush();
						
						BufferedInputStream in = new BufferedInputStream(socket_.getInputStream());
						
						String  responseData  = reaData(in);
						System.out.println(responseData);
						
						bufferOut.write(responseData.getBytes(Charset.forName("UTF-8")));
						bufferOut.flush();
						socket.close();
						socketPool.put(socket_);
					}
					
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

}
