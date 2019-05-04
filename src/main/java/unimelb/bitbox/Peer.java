package unimelb.bitbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

	public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %2$s %4$s: %5$s%n");
		log.info("BitBox Peer starting...");
		Configuration.getConfiguration();

		String[] hostPost = Configuration.getConfigurationValue("peers").split(",");
		HostPort peerAddress = new HostPort(hostPost[0]);

		// create a thread for asClient

		/*new Thread(() -> {
			try {
				asClient(peerAddress.host, peerAddress.port);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}).start();
		System.out.println("already create a thread for asClient");*/

		int localPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
		new Thread(() -> asServer(localPort)).start();


	}

	@SuppressWarnings("unchecked")
	private static void asClient(String ipAddress, int port) throws NumberFormatException, NoSuchAlgorithmException {
		// As a client
		try (Socket socket = new Socket(ipAddress, port)) {
			DataInputStream clientIn = new DataInputStream(socket.getInputStream());
			DataOutputStream clientOut = new DataOutputStream(socket.getOutputStream());

			Document info = new SystemEventMessage().HandShakeRequest(ipAddress, port);

			System.out.println(info.toJson());

			clientOut.writeUTF(info.toJson());
			clientOut.flush();

			// read the data from server
			while (true) {
				if (clientIn.available() > 0) {
					info = Document.parse(clientIn.readUTF());
					// log.config(info.toJson());
					System.out.println(info.toJson());

					if (info.getString("command").equalsIgnoreCase("CONNECTION_REFUSED")) {
						// Parsing the peers and add them into an ArrayList
						ArrayList<Document> peers = new ArrayList<Document>();
						peers.addAll((ArrayList<Document>) info.get("peers"));

						for (int i = 0; i < peers.size(); i++) {
							peersList.add(peers.get(i).get("host") + ":" + peers.get(i).get("port"));
						}
						getpeersList(peersList);

					} else if (info.getString("command").equalsIgnoreCase("HANDSHAKE_RESPONSE")) {
						peersList.clear();
						peersQueue.clear();
						peersMap.clear();
						searchflag = 0;
					}

					new ServerMain(clientOut).HandleFileSystemEvent(info, clientOut);

				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void asServer(int port) {
		// As a server
		ServerSocketFactory socketFactory = ServerSocketFactory.getDefault();
		try (ServerSocket serverSocket = socketFactory.createServerSocket(port)) {
			System.out.println("Waiting for the connection");

			while (true) {
				Socket listenClient = serverSocket.accept();
				// create a new thread for each peer
				new Thread(() -> {
					try {

						serverSocketConnection(listenClient);
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

	private static void serverSocketConnection(Socket client) throws IOException, NoSuchAlgorithmException {
		ArrayList<Document> peerlist = new ArrayList<>();
		try (Socket clientSocket = client) {

			DataInputStream serverIn = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream serverOut = new DataOutputStream(clientSocket.getOutputStream());
			Document serverInfoDocument = new Document();

			int userCount = peerlist.size();
			int maximumIncommingConnections = Integer
					.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
			if (userCount >= maximumIncommingConnections) {
				serverInfoDocument = new SystemEventMessage().connectionRefused(peerlist);

				serverOut.writeUTF(serverInfoDocument.toJson());
			} else {
				String info = serverIn.readUTF();
				serverInfoDocument = Document.parse(info);

				if (!isJSON2(info)) {
					serverInfoDocument = new SystemEventMessage().invalidProtocol();
					serverOut.writeUTF(serverInfoDocument.toJson());
				} else {
					System.out.println("Command Received : " + serverInfoDocument.toJson());

					if (serverInfoDocument.get("hostPort") != null) {
						Document receive = new Document();
						receive = (Document) serverInfoDocument.get("hostPort");
						for (int i = 0; i < peerlist.size(); i++) {
							if (peerlist.get(i).toJson().equals(receive.toJson())) {
								serverInfoDocument = new SystemEventMessage().invalidProtocol();
							}
							// System.out.println(peerlist.get(i).toJson());
						}

						// response server address and port to client.
						serverInfoDocument = new SystemEventMessage().HandShakeResponse();
						serverOut.writeUTF(serverInfoDocument.toJson());
						peerlist.add(receive);
						new ServerMain(serverOut);
						ArrayList<FileSystemEvent> pathevents = ServerMain.fileSystemManager.generateSyncEvents();
						for (FileSystemEvent fileSystemEvent : pathevents) {
							//try {
								Thread thread = new Thread();
								thread.start();
								new ServerMain(serverOut).processFileSystemEvent(fileSystemEvent);

							//} catch (Exception e) {
							//	continue;
							//}						

						}

						while (true) {
							
							if (serverIn.available() > 0) {
								serverInfoDocument = Document.parse(serverIn.readUTF());
								System.out.println("Command Received: " + serverInfoDocument.toJson());
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
		if (searchflag == searchmax)
			return;
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
			return;
		}
	}
}
