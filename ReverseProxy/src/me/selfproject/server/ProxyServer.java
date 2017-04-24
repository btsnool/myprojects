package me.selfproject.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import me.selfproject.constants.CommonConstants;
import me.selfproject.internalserver.InternalProxyServer;

public class ProxyServer {
	
	private String serverIP;
	private int serverPort;
	
	private LinkedBlockingQueue<Socket> socketPool = new LinkedBlockingQueue<Socket>();
	
	
	public ProxyServer(String serverIP , int serverPort){
		
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		
	}
	
	public ProxyServer(){
		
		this.serverIP = "localhost";
		this.serverPort = 80;
		
	}
	
	@SuppressWarnings("resource")
	public void run(){
		
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(serverIP, serverPort));
		} catch (IOException e) {
			System.out.println("server socket bind failed," + e.getMessage());
			return ;
		}
		
		ExecutorService exe = Executors.newFixedThreadPool(100);
		
		while(true){
			try {
				Socket socket = serverSocket.accept();
				exe.submit(new ProxyServerHandle(socket,socketPool));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
		
		
	}
	
	public static void main(String[] args) {
		
//		new ProxyServer("23.235.133.101",80).run();
//		new ProxyServer("localhost",80).run();

		Properties prop = new Properties();
		try {
			prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("conf.properties"));
			System.out.println("system config:");
			for(Object key : prop.keySet()){
				
				System.out.println(key+":"+prop.getProperty((String)key));
				
			}
			new ProxyServer(prop.getProperty("remote.ip"),Integer.valueOf(prop.getProperty("remote.port"))).run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
