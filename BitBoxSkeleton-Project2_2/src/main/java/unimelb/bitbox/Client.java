package unimelb.bitbox;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class Client {
    private static Logger log = Logger.getLogger(Peer.class.getName());

    public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {
        try{
            CommandLineArgs commandLineArgs = new CommandLineArgs();
            CmdLineParser parser = new CmdLineParser(commandLineArgs);
            parser.parseArgument(args);
            String serverHostPort = commandLineArgs.getServerHostport();
            HostPort serverAddress = new HostPort(serverHostPort);

            if(commandLineArgs.getCommand().equalsIgnoreCase("list_peers")){
                new Thread(() -> {
                    try {
                        clientConnectionForPeerList(serverAddress.host, serverAddress.port);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        // TODOAuto-generated catch block
                        e.printStackTrace();
                    }
                }).start();

            }else if(commandLineArgs.getCommand().equalsIgnoreCase("connect_peer")){
                String peer = commandLineArgs.getPeer();
                HostPort peerAddress = new HostPort(peer);
                Document peerHostPort = peerAddress.toDoc();
                new Thread(() -> {
                    try {
                        clientConnectionForConnectPeer(serverAddress.host, serverAddress.port, peerHostPort);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }).start();

            }else if(commandLineArgs.getCommand().equalsIgnoreCase("disconnect_peer")){
                String peer = commandLineArgs.getPeer();
                HostPort peerAddress = new HostPort(peer);
                Document peerHostPort = peerAddress.toDoc();
                new Thread(() -> {
                    try {
                        clientConnectionForDisonnectPeer(serverAddress.host, serverAddress.port, peerHostPort);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }).start();
            }else{
                System.out.println("Command cannot be parsed!");
            }

        }catch (CmdLineException e) {
            System.err.println(e.getMessage());
            //Print the usage to help the user understand the arguments expected
            //by the program
//            parser.printUsage(System.err);
        }
    }

    private static void clientConnectionForPeerList(String ipAddress, int port) throws NumberFormatException, NoSuchAlgorithmException{
        try (Socket socket = new Socket(ipAddress, port)) {
            BufferedWriter clientOut =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));
            boolean flag = true;
//            Document info = new SystemEventMessage().HandShakeRequest(ipAddress, port);

            Document info = new SystemEventMessage().authorizedRequest(Configuration.getConfigurationValue("identity"));

            System.out.println(info.toJson());
            clientOut.write(info.toJson());
            clientOut.newLine();
            clientOut.flush();
            while(flag){
                if(clientIn.ready()){
                    info = Document.parse(clientIn.readLine());
                    System.out.println(info.toJson());
                    if(!info.getString("command").equalsIgnoreCase("AUTH_RESPONSE") || !info.getString("status").equalsIgnoreCase("true")){
                        flag = false;
                    }else{
                        info = new SystemEventMessage().listPeersRequest();
                        System.out.println(info.toJson());
                        clientOut.write(info.toJson());
                        clientOut.newLine();
                        clientOut.flush();
                    }
                }
            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
//
    private static void clientConnectionForConnectPeer(String ipAddress, int port, Document peerHostPort){
        try (Socket socket = new Socket(ipAddress, port)) {
            BufferedWriter clientOut =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));
            boolean flag = true;
//            Document info = new SystemEventMessage().HandShakeRequest(ipAddress, port);

            Document info = new SystemEventMessage().authorizedRequest(Configuration.getConfigurationValue("identity"));

            System.out.println(info.toJson());
            clientOut.write(info.toJson());
            clientOut.newLine();
            clientOut.flush();
            while(flag){
                if(clientIn.ready()){
                    info = Document.parse(clientIn.readLine());
                    System.out.println(info.toJson());
                    if(!info.getString("command").equalsIgnoreCase("AUTH_RESPONSE") || !info.getString("status").equalsIgnoreCase("true")){
                        flag = false;
                    }else{
                        info = new SystemEventMessage().connectPeerRequest(peerHostPort.getString("host"),peerHostPort.getInteger("port"));
                        System.out.println(info.toJson());
                        clientOut.write(info.toJson());
                        clientOut.newLine();
                        clientOut.flush();
                    }
                }
            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private static void clientConnectionForDisonnectPeer(String ipAddress, int port, Document peerHostPort){
        try (Socket socket = new Socket(ipAddress, port)) {
            BufferedWriter clientOut =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));
            boolean flag = true;

            Document info = new SystemEventMessage().authorizedRequest(Configuration.getConfigurationValue("identity"));

            System.out.println(info.toJson());
            clientOut.write(info.toJson());
            clientOut.newLine();
            clientOut.flush();
            while(flag){
                if(clientIn.ready()){
                    info = Document.parse(clientIn.readLine());
                    System.out.println(info.toJson());
                    if(!info.getString("command").equalsIgnoreCase("AUTH_RESPONSE") || !info.getString("status").equalsIgnoreCase("true")){
                        flag = false;
                    }else{
                        info = new SystemEventMessage().disconnectPeerRequest(peerHostPort.getString("host"),peerHostPort.getInteger("port"));
                        System.out.println(info.toJson());
                        clientOut.write(info.toJson());
                        clientOut.newLine();
                        clientOut.flush();
                    }
                }
            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
