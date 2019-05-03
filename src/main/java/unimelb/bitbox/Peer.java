package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

public class Peer {
	private static Logger log = Logger.getLogger(Peer.class.getName());
	private static ArrayList<FileSystemEvent> pathevents;

	

	public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %2$s %4$s: %5$s%n");
		log.info("BitBox Peer starting...");
		Configuration.getConfiguration();

		String hostPost = Configuration.getConfigurationValue("peers");
		HostPort peerAddress = new HostPort(hostPost);
		// ServerMain callserverMain = new ServerMain();

		// create a thread for asClient
		/*new Thread(() -> {
			try {
				asClient(peerAddress.host, peerAddress.port);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start() ;
		System.out.println("already create a thread for asClient");*/

		new Thread(() -> {
			try {
				asServer(8111);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();
		//ServerMain callserverMain = new ServerMain();

		// create a thread for asClient
		// new Thread(() -> asClient(peerAddress.host, peerAddress.port)).start() ;
		// System.out.println("already create a thread for asClient");

		//new Thread(() -> asServer(8000)).start();
		//System.out.println("already create a thread for asServer");

		// asClient(peerAddress.host, peerAddress.port);
		// asClient("45.113.235.184", 8111);

	}

	private static void asClient(String ipAddress, int port) throws NumberFormatException, NoSuchAlgorithmException {
		// As a client
		try (Socket socket = new Socket(ipAddress, port)) {
			BufferedReader clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
			BufferedWriter clientOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
			
			// BufferedReader clientBufferedReader = new BufferedReader(new
			// InputStreamReader(clientIn, "UTF-8"));
			// BufferedWriter clientBufferedWriter = new BufferedWriter(new
			// OutputStreamWriter(clientOut, "UTF-8"));

			Document info = new SystemEventMessage().HandShakeRequest(ipAddress, port);

			System.out.println(info.toJson());

			clientOut.write(info.toJson());
			clientOut.flush();

			// read the data from server
			while (true) {
				if (clientIn.readLine() != null) {
					info = Document.parse(clientIn.readLine());
					System.out.println(info.toJson()+"/r/n");
					new ServerMain(clientOut).HandleFileSystemEvent(info, clientOut);
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void asServer(int port) throws NoSuchAlgorithmException {
		// As a server
		ServerSocketFactory socketFactory = ServerSocketFactory.getDefault();
		try (ServerSocket serverSocket = socketFactory.createServerSocket(port)) {
			System.out.println("Waiting for the connection");

			while (true) {
				Socket listenClient = serverSocket.accept();
				Thread serverThread = new Thread();
				serverThread.start();
				serverSocketConnection(listenClient);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void serverSocketConnection(Socket client) throws IOException, NoSuchAlgorithmException {
		Document serverInfoDocument = new Document();
		try (Socket clientSocket = client) {

			BufferedReader serverIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF-8"));
			BufferedWriter serverOut = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF-8"));
			
			String readServer = serverIn.readLine();
			if(readServer != null) {			
			serverInfoDocument = Document.parse(readServer);
			System.out.println("Command Received : " + serverInfoDocument.toJson() );

			// response server address and port to client.
			serverInfoDocument = new SystemEventMessage().HandShakeResponse();
			serverOut.write(serverInfoDocument.toJson());
		 
			// start to synchronous
			pathevents = new ServerMain(serverOut).fileSystemManager.generateSyncEvents();
			for (FileSystemEvent fileSystemEvent : pathevents) {
				new Thread(() -> {
					try {
						new ServerMain(serverOut).processFileSystemEvent(fileSystemEvent);
						//serverObject.HandleFileSystemEvent(fileSystemManager, serverInfoDocument, serverOut);
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				}).start();
			}
			/*// start to synchronous
			 * 

			int timeInterval = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));
			new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(timeInterval * 100); // set time to 60s
						pathevents = new ServerMain(serverOut).fileSystemManager.generateSyncEvents();
						for (FileSystemEvent fileSystemEvent : pathevents) {
							new Thread(() -> {
								try {
									new ServerMain().processFileSystemEvent(fileSystemEvent);
								} catch (NumberFormatException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (NoSuchAlgorithmException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}).start();
							;
						}

					} catch (InterruptedException | NumberFormatException | NoSuchAlgorithmException | IOException e) {
						e.printStackTrace();
					}

				}

			}).start();

			}).start();*/

			while (true) {
				if (serverIn.readLine() != null) {
					serverInfoDocument = Document.parse(serverIn.readLine());
					System.out.println("Command Received: " + serverInfoDocument.toJson());
					new ServerMain(serverOut).HandleFileSystemEvent(serverInfoDocument, serverOut);

				}

			}

		}}
	}
}
