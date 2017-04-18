package me.selfproject.internalserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;

import me.selfproject.constants.CommonConstants;

public class InternalProxyServer {
	
	private String remoteServerIP;
	private int remoteServerPort;
	private final static int INIT_CONN_SIZE = 4;
	
	
	public InternalProxyServer(String remoteServerIP , int remoteServerPort){
		
		this.remoteServerIP = remoteServerIP;
		this.remoteServerPort = remoteServerPort;
		
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
			}
			
		}
		return new String(data,"UTF-8");
	}
	
	@SuppressWarnings("resource")
	public void run(){
		
		Socket socket = new Socket();
		
		try {
			socket.connect(new InetSocketAddress(remoteServerIP, remoteServerPort));
			BufferedOutputStream  bufferOut  = new BufferedOutputStream(socket.getOutputStream());
			
			BufferedInputStream bufferIn = new BufferedInputStream(socket.getInputStream());
			
			bufferOut.write(CommonConstants.MSG_NEWCONN.getBytes());
			bufferOut.write(CommonConstants.MSG_SPLIT.getBytes());
			bufferOut.flush();
			
			while(true){
					
				String msg = reaData(bufferIn);
				
				System.out.println(msg);
				
				if((CommonConstants.MSG_OK+CommonConstants.MSG_SPLIT).equals(msg)){
					
					
				}else{
					
					Socket redirectSocket = new Socket();
					
					redirectSocket.connect(new InetSocketAddress("183.134.5.17", 9080));
					
					BufferedOutputStream redirectOufferOut = new BufferedOutputStream(redirectSocket.getOutputStream());
					redirectOufferOut.write(msg.getBytes(Charset.forName("UTF-8")));
					redirectOufferOut.flush();
					
					BufferedInputStream redirectBufferIn_ = new BufferedInputStream(redirectSocket.getInputStream());

					bufferOut.write(reaData(redirectBufferIn_).getBytes(Charset.forName("UTF-8")));
					bufferOut.flush();
//					redirectSocket.close();
					
				}
				
				
			}
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	public static void main(String[] args) {
		
		new InternalProxyServer("localhost", 80).run();

	}

}
