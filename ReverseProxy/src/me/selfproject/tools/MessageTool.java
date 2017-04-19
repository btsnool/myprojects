package me.selfproject.tools;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.selfproject.constants.CommonConstants;

public class MessageTool {

	
	private static boolean checkIsStreamEnd(byte[] by){
			
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
	private static  boolean checkContentLength(byte[] content){
		
		try {
			String tmpString = new String(content,"UTF-8");
			
	//		System.out.println("content----"+tmpString);
			
			Pattern pattern = Pattern.compile("(?<=Content-Length: )\\d+");
			
			Matcher ma = pattern.matcher(tmpString);
			
			if(ma.find()){
				
				int contentLength = Integer.valueOf(ma.group());
				
//				Pattern messageBodyReg = Pattern.compile("(?<=\r\n\r\n)(.*[\r\n]{1,2})*");
//				
//				Matcher ma_ = messageBodyReg.matcher(tmpString);
//				
//				if(ma_.find()){
//					
//					String messageBody = ma_.group();
//	//				System.out.println(messageBody);
//					System.out.println("content-length : " + contentLength+";actual size:"+messageBody.length());
//					if(messageBody.length() == contentLength || messageBody.length()+1 == contentLength){
//						return true;
//					}
//				}
				int messageBodyLength = getMessageBodyLength(content);
				System.out.println("content-length : " + contentLength+";actual size:"+messageBodyLength);
				if(contentLength == messageBodyLength){
					return true;
					
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
	
	/**
	 * 检查HTTP Message是否已经读取完全
	 * @param content
	 * @return
	 */
	private static int getMessageBodyLength(byte[] content){
		
		
		int idx = 0 ; 
		
		boolean isFound = false;
		
		while(idx<content.length){
			
			if(content[idx]=='\r' && idx+3<content.length){
				
				if(content[idx+1] == '\n' && content[idx+2] == '\r' && content[idx+3] == '\n'){
					
					break;
				}
				
			}
			idx++;
			
		}
		if(idx!=content.length){
			
			return content.length-idx-4;
		}
		return 0;
		
	}
	
	
	public  static  String readData(InputStream in , String charset) throws Exception{
		
		byte[] data = readData(in);
		
		if(charset ==null){
			charset = "UTF-8";
		}
		
		return new String(data,charset);
		
	}
	public  static  byte[] readData(InputStream in) throws Exception{
		
		
		byte[] data = null;
		
		boolean endOfRequest = false;
		while(!endOfRequest){
			
			byte[] buffer = new byte[1024*1024];
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
//		System.out.println("data size:"+data.length);
		return data;
		
	}

}
