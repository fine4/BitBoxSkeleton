package unimelb.bitbox;

import java.io.*;
import java.net.*;
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
            System.out.println("already create a thread for asClient");

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

            // create a thread for asServerForClient
            int clientPort = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
            new Thread(() -> {
                asServerForClient(clientPort);
            }).start();

        }else if(Configuration.getConfigurationValue("mode").equalsIgnoreCase("udp")){
           //UDP mode
            //create udpClient
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

            //create udpServer
            String localHost = Configuration.getConfigurationValue("advertisedName");
            new Thread(()->{
                try{asUDPServer(localHost, 8111);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }).start();
        }


	}
    private static void asUDPServer(String host, int port) throws IOException{
        System.out.println("Waiting for the connection");
        int dataSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
        byte[] buffer = new byte[dataSize];
        InetSocketAddress socketAddress = new InetSocketAddress(host,port);
        DatagramSocket datagramSocket = new DatagramSocket(socketAddress);

        DatagramPacket datagramPacketReceive = new DatagramPacket(buffer,dataSize);

        while(true){
            //bind
            datagramSocket.receive(datagramPacketReceive);

            String x = new String(datagramPacketReceive.getData(),0,datagramPacketReceive.getLength());
            Document info = Document.parse(x);
            Document responseInfo = new SystemEventMessage().authorizedResponseSuccess();
            System.out.println(responseInfo.toJson());
            DatagramPacket datagramPacketSend = new DatagramPacket(responseInfo.toJson().getBytes(),responseInfo.toJson().length(),datagramPacketReceive.getSocketAddress());
            datagramSocket.send(datagramPacketSend);
            System.out.println(info.toJson());
        }

    }

    private static void asUDPClient(String ipAddress, int port) throws IOException, SocketException, NumberFormatException, NoSuchAlgorithmException{

//		int timeOut = Integer.parseInt(Configuration.getConfigurationValue("timeOut"));
        int localPort = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
        String localhost = Configuration.getConfigurationValue("advertisedName");
        int maximumTries = Integer.parseInt(Configuration.getConfigurationValue("maximumTries"));
        int blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
        byte[] buffer = new byte[blockSize];

        InetSocketAddress socketAddress = new InetSocketAddress(localhost, localPort);
        DatagramSocket datagramSocket = new DatagramSocket(socketAddress);

        Document info = new SystemEventMessage().authorizedRequest(Configuration.getConfigurationValue("identity"));

        DatagramPacket datagramPacketSend = new DatagramPacket(info.toJson().getBytes(),info.toJson().length());

        datagramPacketSend.setAddress(InetAddress.getByName(ipAddress));
        datagramPacketSend.setPort(port);
        DatagramPacket datagramPacketReceive = new DatagramPacket(buffer,blockSize);

        int tries = 0;
        boolean receiveResponse = false;
        while(!receiveResponse && tries<maximumTries){
            datagramSocket.send(datagramPacketSend);
            System.out.println(info.toJson());
            datagramSocket.receive(datagramPacketReceive);

            try{
                String receiveAddress = datagramPacketReceive.getAddress().toString().substring(1,datagramPacketReceive.getAddress().toString().length());
                if (!receiveAddress.equalsIgnoreCase(ipAddress)){
                    throw new IOException("Received packet from an unknown source");
                }
                receiveResponse = true;
            }catch(InterruptedIOException e){
                tries = tries + 1;

            }

        }
        if(receiveResponse){
            String x = new String(datagramPacketReceive.getData(),0,datagramPacketReceive.getLength());
            Document receiveInfo = Document.parse(x);
            System.out.println(receiveInfo.toJson());
            datagramPacketReceive.setLength(blockSize);
        }
        datagramSocket.close();
    }


	@SuppressWarnings("unchecked")
	private static void asClient(String ipAddress, int port) throws NumberFormatException, NoSuchAlgorithmException {
		// As a client
		try (Socket socket = new Socket(ipAddress, port)) {
			BufferedWriter clientOut =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
			BufferedReader clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));
			BufferedWriter clientOut2 = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream(),"UTF8"));
			//DataInputStream clientIn = new DataInputStream(socket.getInputStream());
			//DataOutputStream clientOut = new DataOutputStream(socket.getOutputStream());

			Document info = new SystemEventMessage().HandShakeRequest(ipAddress, port);

			System.out.println(info.toJson());
			clientOut.write(info.toJson());
			clientOut.newLine();
			clientOut.flush();


			// read the data from server
			while (true) {
				if (clientIn.ready()) {
					info = Document.parse(clientIn.readLine());
					System.out.println(info.toJson());

					if (info.getString("command").equalsIgnoreCase("CONNECTION_REFUSED")) {
						// Parsing the peers and add them into an ArrayList
						Document infoToClient = new SystemEventMessage().connectPeerResponseFail(ipAddress,port);
						clientOut2.write(infoToClient.toJson());
						clientOut2.newLine();
						clientOut2.flush();

						peersMap.put(ipAddress + ":" + port, Math.random());
						ArrayList<Document> peers = new ArrayList<Document>();
						peers.addAll((ArrayList<Document>) info.get("peers"));

						for (int i = 0; i < peers.size(); i++) {
							peersList.add(peers.get(i).get("host") + ":" + peers.get(i).get("port"));
						}
						getpeersList(peersList);

					} else if (info.getString("command").equalsIgnoreCase("HANDSHAKE_RESPONSE")) {
						Document infoToClient = new SystemEventMessage().connectPeerResponseSuccess(ipAddress,port);
						clientOut2.write(infoToClient.toJson());
						clientOut2.newLine();
						clientOut2.flush();
						peersList.clear();
						peersQueue.clear();
						peersMap.clear();
						searchflag = 0;
					}
					if(info.getString("command").equalsIgnoreCase("DISCONNECT_REQUEST")){
						socket.close();
					}
					new ServerMain(clientOut).HandleFileSystemEvent(info, clientOut);
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
			System.out.println("Waiting for the connection");
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
			Queue<String> queueRead = new LinkedList<String>();
			int userCount = peerlist.size();
			int maximumIncommingConnections = Integer
					.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
			
			if (userCount >= maximumIncommingConnections) {
				serverInfoDocument = new SystemEventMessage().connectionRefused(peerlist);
				serverOut.write(serverInfoDocument.toJson());
				serverOut.newLine();
				serverOut.flush();
			} else {
				
				String info = serverIn.readLine();
				serverInfoDocument = Document.parse(info);
				if (!isJSON2(info)) {
					serverInfoDocument = new SystemEventMessage().invalidProtocol();
					serverOut.write(serverInfoDocument.toJson());
					serverOut.newLine();
					serverOut.flush();
				} else {
					System.out.println(serverInfoDocument.toJson());
					if (serverInfoDocument.get("hostPort") != null) {
						Document receive = new Document();
						receive = (Document) serverInfoDocument.get("hostPort");
						for (int i = 0; i < peerlist.size(); i++) {
							if (peerlist.get(i).toJson().equals(receive.toJson())) {
								serverInfoDocument = new SystemEventMessage().invalidProtocol();
							}
						}
						if(serverInfoDocument.getString("command").equalsIgnoreCase("HANDSHAKE_REQUEST")){
							serverInfoDocument = new SystemEventMessage().HandShakeResponse();
							serverOut.write(serverInfoDocument.toJson());
							serverOut.newLine();
							serverOut.flush();
							peerlist.add(receive);
						}
						if(serverInfoDocument.getString("command").equalsIgnoreCase("DISCONNECT_REQUEST")){
							clientSocket.close();
							peerlist.remove(receive);
						}
						new ServerMain(serverOut);

						new Thread(()->{
							try {
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
							if (serverIn.ready() ) {
								if((string = serverIn.readLine())!= null) {
										//queueRead.offer(string);
										//for (String stringRead : queueRead) {
											serverInfoDocument = Document.parse(serverIn.readLine());
											System.out.println(serverInfoDocument.toJson());
											new ServerMain(serverOut).HandleFileSystemEvent(serverInfoDocument, serverOut);
											//queueRead.poll();
										//}
									}
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
								Socket disSocket = new Socket(hostD,portD);
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
			if (item.getString("host").equalsIgnoreCase(host)&&item.getInteger("port")==port){
				connect = true;
				break;
			}
		}
		return connect;
	}
	
	

	

}