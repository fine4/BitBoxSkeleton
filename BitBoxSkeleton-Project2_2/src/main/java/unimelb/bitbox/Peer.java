package unimelb.bitbox;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import com.alibaba.fastjson.JSONObject;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

public class Peer {
	private static Logger log = Logger.getLogger(Peer.class.getName());

	private static ArrayList<String> peersList = new ArrayList<>();
	private static Queue<String> peersQueue = new LinkedList<>();
	private static HashMap peersMap = new HashMap();
	private static int searchflag = 0;
	private static final int searchmax = 10;
	private static ArrayList<Document> peerlist = new ArrayList<>();
	static Socket cSocket;
	//private static Queue <FileSystemEvent> eventQueue = new LinkedList<>();

	public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %2$s %4$s: %5$s%n");
		log.info("BitBox Peer starting...");
		Configuration.getConfiguration();

		String[] hostPost = Configuration.getConfigurationValue("peers").split(",");
		HostPort peerAddress = new HostPort(hostPost[0]);
        if(Configuration.getConfigurationValue("mode").equalsIgnoreCase("tcp") ){

             // create a thread for asServer
            int localPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
            new Thread(() -> {
                try {
                    asServer(localPort);
                } catch (NoSuchAlgorithmException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }).start();

			//create a thread for asClient
			new Thread(() -> {
				try {
					asClient(peerAddress.host, peerAddress.port);
				} catch (NumberFormatException e) {

					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODOAuto-generated catch block
					e.printStackTrace();
				}
			}).start();
			log.info("already create a thread for asClient");


			// create a thread for asServerForClient
            int clientPort = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
            new Thread(() -> {
                asServerForClient(clientPort);
            }).start();

        }else if(Configuration.getConfigurationValue("mode").equalsIgnoreCase("udp")){
           //UDP mode
            //create udpServer
            int localPort = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
            new Thread(()->{
                try{
                	asUDPServer(localPort);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }).start();

			//create udpClient
			String localHost = Configuration.getConfigurationValue("advertisedName");
			int Port = Integer.parseInt(Configuration.getConfigurationValue("port"));
			new Thread(()->{
				try{
					asUDPClient(peerAddress.host,peerAddress.port);
				}catch (NumberFormatException e){
					e.printStackTrace();
				}catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}catch (IOException e){
					e.printStackTrace();
				}
			}).start();
        }
	}

    private static void asUDPServer(int port) throws IOException{
        System.out.println("Waiting for the connection");
        try(DatagramSocket datagramSocket = new DatagramSocket(port)){
			int dataSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
			byte[] buffer = new byte[dataSize];
			DatagramPacket datagramPacketReceive = new DatagramPacket(buffer,dataSize);

			while(true){
				datagramSocket.receive(datagramPacketReceive);

				String receiveContent = new String(datagramPacketReceive.getData(),0,datagramPacketReceive.getLength());
				Document info = Document.parse(receiveContent);
				System.out.println("server"+info.toJson());
				if(info.getString("command").equalsIgnoreCase("HANDSHAKE_REQUEST")){
					Document responseInfo = new SystemEventMessage().HandShakeResponse();
					System.out.println("server"+responseInfo.toJson());
					DatagramPacket datagramPacketSend = new DatagramPacket(responseInfo.toJson().getBytes(),responseInfo.toJson().length(),datagramPacketReceive.getSocketAddress());
					datagramSocket.send(datagramPacketSend);
				}


			}
		}
    }

    private static void asUDPClient(String ipAddress, int port) throws IOException, SocketException, NumberFormatException, NoSuchAlgorithmException{
//		System.out.println("Client want to connect");
		InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, port);
//		int timeOut = Integer.parseInt(Configuration.getConfigurationValue("timeOut"));
		try(DatagramSocket datagramSocket = new DatagramSocket(Integer.parseInt(Configuration.getConfigurationValue("port")))){
			int maximumTries = Integer.parseInt(Configuration.getConfigurationValue("maximumTries"));
			int blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize")); //传输的blockSize大小
			byte[] buffer = new byte[blockSize];

			Document info = new SystemEventMessage().HandShakeRequest(ipAddress,port);
			//datagramPacket for send/receive packet
			DatagramPacket datagramPacketSend = new DatagramPacket(info.toJson().getBytes(),info.toJson().length(), socketAddress.getAddress(), socketAddress.getPort());
			DatagramPacket datagramPacketReceive = new DatagramPacket(buffer,blockSize);
			//send packet
			datagramSocket.send(datagramPacketSend);
			System.out.println("Client"+info.toJson());

			while(true){
				//receive packet
				datagramSocket.receive(datagramPacketReceive);
				String y = new String(datagramPacketReceive.getData(),0,datagramPacketReceive.getLength());
				Document receiveInfo = Document.parse(y);
				System.out.println("Client"+receiveInfo.toJson());

				if(receiveInfo.getString("command").equalsIgnoreCase("HANDSHAKE_RESPONSE")){
					//这里应该是文件传输了吧
				}
			}



//			重传叭
//			int tries = 0;
//			boolean receiveResponse = false;
//        	while(!receiveResponse && tries<maximumTries){
////				datagramSocket.send(datagramPacketSend);
////				System.out.println(info.toJson());
//				datagramSocket.receive(datagramPacketReceive);
//
//				try{
//					String receiveAddress = datagramPacketReceive.getAddress().toString().substring(1,datagramPacketReceive.getAddress().toString().length());
//					if (!receiveAddress.equalsIgnoreCase(ipAddress)){
//						throw new IOException("Received packet from an unknown source");
//					}
//					receiveResponse = true;
//				}catch(InterruptedIOException e){
//					tries = tries + 1;
//
//				}
//
//			}
//        	if(receiveResponse){
//				String x = new String(datagramPacketReceive.getData(),0,datagramPacketReceive.getLength());
//				Document receiveInfo = Document.parse(x);
//				System.out.println(receiveInfo.toJson());
//				datagramPacketReceive.setLength(blockSize);
//			}
//			datagramSocket.close();
		}
	}


	@SuppressWarnings("unchecked")
	private static void asClient(String ipAddress, int port) throws NumberFormatException, NoSuchAlgorithmException {
		// As a client
		try (Socket socket = new Socket(ipAddress, port)) {
			BufferedWriter clientOut =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
			BufferedReader clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));

			//DataInputStream clientIn = new DataInputStream(socket.getInputStream());
			//DataOutputStream clientOut = new DataOutputStream(socket.getOutputStream());

			Document info = new SystemEventMessage().HandShakeRequest(ipAddress, port);
			Document receive = new Document();
			log.info(info.toJson());
			clientOut.write(info.toJson());
			clientOut.newLine();
			clientOut.flush();


			// read the data from server
			while (true) {
				String string = null;
				if (clientIn.ready()) {
					if ((string = clientIn.readLine()) != null) {
						info = Document.parse(string);
						log.info(string);

						if (info.getString("command").equalsIgnoreCase("CONNECTION_REFUSED")) {
							// Parsing the peers and add them into an ArrayList
							if(cSocket!=null){
								BufferedWriter clientOut2 = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream(),"UTF8"));
								Document infoToClient = new SystemEventMessage().connectPeerResponseFail(ipAddress,port);
								clientOut2.write(infoToClient.toJson());
								clientOut2.newLine();
								clientOut2.flush();
							}

							peersMap.put(ipAddress + ":" + port, Math.random());
							ArrayList<Document> peers = new ArrayList<Document>();
							peers.addAll((ArrayList<Document>) info.get("peers"));

							for (int i = 0; i < peers.size(); i++) {
								peersList.add(peers.get(i).get("host") + ":" + peers.get(i).get("port"));
							}
							getpeersList(peersList);

						} else if (info.getString("command").equalsIgnoreCase("HANDSHAKE_RESPONSE")) {
							if(cSocket!=null){
								BufferedWriter clientOut2 = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream(),"UTF8"));
								Document infoToClient = new SystemEventMessage().connectPeerResponseSuccess(ipAddress,port);
								clientOut2.write(infoToClient.toJson());
								clientOut2.newLine();
								clientOut2.flush();
							}
							receive = (Document) info.get("hostPort");
							peerlist.add(receive);
							peersList.clear();
							peersQueue.clear();
							peersMap.clear();
							searchflag = 0;
						}
						if(info.getString("command").equalsIgnoreCase("DISCONNECT_REQUEST")){
							info = new SystemEventMessage().DisconnectResponse();
							System.out.println("client"+info.toJson());
							clientOut.write(info.toJson());
							clientOut.newLine();
							clientOut.flush();

						}
						if(info.getString("command").equalsIgnoreCase("DISCONNECT_RESPONSE")){
							socket.shutdownInput();
							socket.shutdownOutput();
							socket.close();
							peerlist.remove(receive);
							if(cSocket!=null){
								BufferedWriter clientOut2 = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream(),"UTF8"));
								Document infoToClient = new SystemEventMessage().disconnectPeerResponseSuccess(ipAddress,port);
								clientOut2.write(infoToClient.toJson());
								clientOut2.newLine();
								clientOut2.flush();
							}

						}
						new ServerMain(clientOut).HandleFileSystemEvent(info, clientOut);

					}
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void asServer(int port) throws NoSuchAlgorithmException, InterruptedException {
		// As a server
		ServerSocketFactory socketFactory = ServerSocketFactory.getDefault();
		try (ServerSocket serverSocket = socketFactory.createServerSocket(port)) {
			log.info("Peer is waiting for the connection");
			while (true) {
				Socket listenClient = serverSocket.accept();
				new Thread(()->{
					try {
						serverSocketConnection(listenClient);
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}).start();
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void asServerForClient(int port) {
		ServerSocketFactory socketFactory = ServerSocketFactory.getDefault();
		try (ServerSocket serverSocket = socketFactory.createServerSocket(port)) {
			System.out.println("Waiting for the connection for client...");
			while (true) {
				Socket listenClient = serverSocket.accept();
				// create a new thread for each peer
				new Thread(() -> {
					try {
						serverClientSocketConnection(listenClient);
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	private static void serverSocketConnection(Socket client) throws IOException, NoSuchAlgorithmException, InterruptedException {

		try (Socket clientSocket = client) {
			
			BufferedReader serverIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF8"));
			BufferedWriter serverOut = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF8"));
			
			Document serverInfoDocument = new Document();

			int userCount = peerlist.size();
			int maximumIncommingConnections = Integer
					.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
			
			if (userCount >= maximumIncommingConnections) {
				serverInfoDocument = new SystemEventMessage().connectionRefused(peerlist);
				serverOut.write(serverInfoDocument.toJson());
				serverOut.newLine();
				serverOut.flush();
			}else {
					String info = null;
					if ((info = serverIn.readLine()) != null) {
						serverInfoDocument = Document.parse(info);

						if (!isJSON2(info)) {
							serverInfoDocument = new SystemEventMessage().invalidProtocol();
							serverOut.write(serverInfoDocument.toJson());
							serverOut.newLine();
							serverOut.flush();
						} else {
							log.info(serverInfoDocument.toJson());
							boolean exist = false;
							System.out.println("server:"+serverInfoDocument.toJson());
							Document peer1 = new Document();
							peer1 = (Document) serverInfoDocument.get("hostPort");
							for (Document item : peerlist){
								if(item.toJson().equals(peer1.toJson())){
									exist = true;
								}
							}
							if(serverInfoDocument.getString("command").equalsIgnoreCase("HANDSHAKE_REQUEST")&&exist == true){
								Long port2 = peer1.getLong("port");
								int removePort = port2.intValue();
								serverInfoDocument = new SystemEventMessage().DisconnectRequest(peer1.getString("host"),removePort);
								System.out.println("Server："+serverInfoDocument.toJson());
								serverOut.write(serverInfoDocument.toJson());
								serverOut.newLine();
								serverOut.flush();

							}
							if (serverInfoDocument.get("hostPort") != null) {
								Document receive = new Document();
								receive = (Document) serverInfoDocument.get("hostPort");
								for (int i = 0; i < peerlist.size(); i++) {
									if (peerlist.get(i).toJson().equals(receive.toJson())) {
										serverInfoDocument = new SystemEventMessage().invalidProtocol();
									}
								}
	                            if(serverInfoDocument.getString("command").equalsIgnoreCase("HANDSHAKE_REQUEST")&&exist!=true){
	                                serverInfoDocument = new SystemEventMessage().HandShakeResponse();
	                                serverOut.write(serverInfoDocument.toJson());
	                                serverOut.newLine();
	                                serverOut.flush();
	                                peerlist.add(receive);
	                            }
								if(serverInfoDocument.getString("command").equalsIgnoreCase("DISCONNECT_RESPONSE")){
									clientSocket.shutdownInput();
									clientSocket.shutdownOutput();
									clientSocket.close();
									peerlist.remove(receive);
									if(cSocket!=null){
										BufferedWriter serverOut2 = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream(),"UTF8"));
										Document infoToClient = new SystemEventMessage().disconnectPeerResponseSuccess(receive.getString("host"),receive.getInteger("port"));
										System.out.println("Server："+serverInfoDocument.toJson());
										serverOut2.write(infoToClient.toJson());
										serverOut2.newLine();
										serverOut2.flush();
									}

								}
						}
					}

					new Thread(() -> {
						try {
							new ServerMain(serverOut);
							timingSynv(serverOut);
						} catch (NumberFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}).start();

					String string = null;
					while (true) {
						if (serverIn.ready()) {
							if ((string = serverIn.readLine()) != null) {
								log.info(string);
								serverInfoDocument = Document.parse(string);						
								new ServerMain(serverOut).HandleFileSystemEvent(serverInfoDocument, serverOut);
							}
						}
					}
				}
			}
		}
	}
	public static boolean isJSON2(String string) {
		boolean result = false;
		try {
			Object object = JSONObject.parse(string);
			result = true;
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	private static void getpeersList(ArrayList<String> peers) {
		searchflag++;
		if (searchflag == searchmax) {
			peersList.clear();
			peersQueue.clear();
			peersMap.clear();
			searchflag = 0;
			return;
		}
		String[] hostport;
		String host;
		String portstring;
		int port;
		for (String peer : peers) {
			System.out.println(4);
			if (peersMap == null || (!peersMap.containsKey(peer) && !peersMap.containsValue(peer))) {
				peersMap.put(peer, Math.random());
				peersQueue.add(peer);
			}
		}
		if (peersQueue != null) {
			hostport = peersQueue.element().split(":");
			host = hostport[0];
			portstring = hostport[1];
			port = Integer.parseInt(portstring);
			peersQueue.remove();
			try {
				asClient(host, port);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			peersList.clear();
			peersQueue.clear();
			peersMap.clear();
			searchflag = 0;
			return;
		}
	}
	
	public static void timingSynv (BufferedWriter serverOut) throws NumberFormatException, NoSuchAlgorithmException, IOException, InterruptedException {
		ArrayList<FileSystemEvent> pathevents = new ArrayList<FileSystemEvent>();
		while(true) {
			
			synchronized (pathevents) {
				pathevents = ServerMain.fileSystemManager.generateSyncEvents();
			}
			for (FileSystemEvent fileSystemEvent : pathevents) {
				new ServerMain(serverOut).processFileSystemEvent(fileSystemEvent);
			}
			
			Thread.sleep(60000);
		}


	}

	private static void serverClientSocketConnection(Socket client) throws IOException, NoSuchAlgorithmException{
		ArrayList<Document> peerlist = new ArrayList<>();
		try (Socket clientSocket = client) {

			BufferedReader serverIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF8"));
			BufferedWriter serverOut = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF8"));
			Document serverInfoDocument = new Document();

			while(true){
				if(serverIn.ready()){
					String info = serverIn.readLine();
					serverInfoDocument = Document.parse(info);
					System.out.println(serverInfoDocument.toJson());

					switch (serverInfoDocument.getString("command")){
						case "AUTH_REQUEST":
							String[] authList = Configuration.getConfigurationValue("authorized_keys").split(",");
							for(String item : authList ){
								if(item.contains(serverInfoDocument.getString("identity"))){
									serverInfoDocument = new SystemEventMessage().authorizedResponseSuccess();
									serverOut.write(serverInfoDocument.toJson());
									serverOut.newLine();
									serverOut.flush();
									System.out.println(serverInfoDocument.toJson());
								}else{
									serverInfoDocument = new SystemEventMessage().authorizedResponseFail();
									serverOut.write(serverInfoDocument.toJson());
									serverOut.newLine();
									serverOut.flush();
									System.out.println(serverInfoDocument.toJson());
								}
							}
							break;
						case "LIST_PEERS_REQUEST":
							serverInfoDocument = new SystemEventMessage().listPeersResponse(peerlist);
							serverOut.write(serverInfoDocument.toJson());
							serverOut.newLine();
							serverOut.flush();
							System.out.println(serverInfoDocument.toJson());
							break;
						case "CONNECT_PEER_REQUEST":
							String hostC = serverInfoDocument.getString("host");
							Long x = serverInfoDocument.getLong("port");
							int portC = x.intValue();
							boolean connect = peerInConnectedList(hostC, portC);
							if(connect){
								serverInfoDocument = new SystemEventMessage().connectPeerResponseSuccess(hostC,portC);
								serverOut.write(serverInfoDocument.toJson());
								serverOut.newLine();
								serverOut.flush();
							}else{
								new Thread(() -> {
									try {
										cSocket = clientSocket;
										asClient(hostC, portC);
									} catch (NumberFormatException e) {
										e.printStackTrace();
									} catch (NoSuchAlgorithmException e) {
										// TODOAuto-generated catch block
										e.printStackTrace();
									}
								}).start();
							}

							break;
						case "DISCONNECT_PEER_REQUEST":
							String hostD = serverInfoDocument.getString("host");
							Long y = serverInfoDocument.getLong("port");
							int portD = y.intValue();
							boolean disconnect = peerInConnectedList(hostD, portD);
							if(!disconnect){
								serverInfoDocument = new SystemEventMessage().disconnectPeerResponseFail(hostD,portD);
								serverOut.write(serverInfoDocument.toJson());
								serverOut.newLine();
								serverOut.flush();
							}else{

								try {
									cSocket = clientSocket;
									asClient(hostD, portD);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								} catch (NoSuchAlgorithmException e) {
									// TODOAuto-generated catch block
									e.printStackTrace();
								}

							}
							break;
						default:
							break;
					}
				}
			}


		}
	}

	private static boolean peerInConnectedList(String host, int port){
		boolean connect = false;
		for(Document item : peerlist){
			Long test = item.getLong("port");
			int testPort = test.intValue();
			if (item.getString("host").equalsIgnoreCase(host)&&testPort==port){
				connect = true;
				break;
			}
		}
		return connect;
	}

}