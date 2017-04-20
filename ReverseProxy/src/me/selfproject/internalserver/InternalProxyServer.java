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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.selfproject.constants.CommonConstants;
import me.selfproject.tools.MessageTool;

public class InternalProxyServer {
	
	private String remoteServerIP;
	private int remoteServerPort;
	private final static int INIT_CONN_SIZE = 20;
	
	public InternalProxyServer(String remoteServerIP , int remoteServerPort){
		
		this.remoteServerIP = remoteServerIP;
		this.remoteServerPort = remoteServerPort;
		
	}
	
	@SuppressWarnings("resource")
	public void run(){
		
		ExecutorService exe = Executors.newFixedThreadPool(INIT_CONN_SIZE);
		
		for(int i=0 ; i<INIT_CONN_SIZE ; i++){
			
			exe.submit(new InternalProxyServerHandle(remoteServerIP, remoteServerPort));
			
		}
		
	}
	
	

	public static void main(String[] args) {
		
//		new InternalProxyServer("localhost", 80).run();
		new InternalProxyServer("23.235.133.101", 80).run();
		

	}

}
