package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

import org.json.simple.parser.ParseException;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class Client {
	private static String ipAddress = "localhost";
	private static int port = 8000;
	FileSystemManager fileSystemManager;
	static ArrayList<FileSystemEvent> fileEventRecv;
	private static final int buffer_size = 1024;
	
	public static void main(String[] args) throws ParseException, IOException {

		try (Socket socket = new Socket(ipAddress, port)) {
			DataInputStream clientIn = new DataInputStream(socket.getInputStream());
			DataOutputStream clientOut = new DataOutputStream(socket.getOutputStream());
			BufferedReader clientBufferedReader = new BufferedReader(new InputStreamReader(clientIn, "UTF-8"));
			
			Document info = new Document();
			info = new SystemEventMessage().HandShakeRequest(ipAddress, port);
			
			String jsonCommand = info.toJson();
			System.out.println(jsonCommand);
			
			// send RMI to server
			clientOut.writeUTF(jsonCommand);
			clientOut.flush(); 
			
			// read the data from server
			while (true) {
				if(clientIn.available() > 0) {
					info = Document.parse(clientIn.readUTF());
					System.out.println(info.toJson());
				}
			}
			//while (clientBufferedReader.readLine() != null) {
				
				
			//}
			/*char[] data = new char[buffer_size];
			// info = Document.parse(clientIn.readUTF());
			int length = clientBufferedReader.read(data);
			String recvMessage = String.valueOf(data, 0, length);
			// String messageFromServer = clientIn.readUTF();
			System.out.println("received message is " + recvMessage);

			
			//}*/
			
			
			/*try {
				fileEventRecv = new ServerMain().fileSystemManager.generateSyncEvents();
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (FileSystemEvent fileSystemEvent : fileEventRecv) {
				if (fileSystemEvent.event == EVENT.FILE_CREATE) {
					info.append("Command", "FILE_CREATE_REQUEST");
					info.append("FileDescriptor", fileSystemEvent.fileDescriptor.toDoc());
					info.append("pathName", fileSystemEvent.pathName);
					clientOut.writeUTF(info.toJson());
					System.out.println(info.toJson());
				}
				
			}
			System.out.println("fileEvent"+ fileEventRecv);
			//info.append(key, val);

			
			clientIn.close();
			clientOut.close();
			socket.close();*/
			

			//info.append("pathName", "C:\\Users\\123\\Desktop\\07566780.pdf");
			//info.append("fileName", "07566780.pdf");


			// print out results received from the server

			/*
			 * JSONParser jsonParser = new JSONParser(); while (true) { if
			 * (clientIn.available() >0) { String resultFromServer = clientIn.readUTF();
			 * System.out.println("Received from the server:" + resultFromServer);
			 * JSONObject parserCommand = (JSONObject) jsonParser.parse(resultFromServer);
			 * 
			 * // check the command name if (parserCommand.containsKey("Command_name")) { if
			 * (parserCommand.get("Command_name").equals("DOWNLOAD_FILE_FROM_SERVER")) {
			 * 
			 * // get the file location String fileName =
			 * "/Users/shizhi/Desktop/TestClient/" + parserCommand.get("File_name");
			 * 
			 * // Create a RandomAccessFile to read and write the output file.
			 * RandomAccessFile downloadFileFromServer = new RandomAccessFile(fileName,
			 * "rw");
			 * 
			 * // Find out how much size is remaining to get from the server. long
			 * fileSizeRemaining = (Long) parserCommand.get("file_size");
			 * 
			 * int chunkSize = setChunkSize(fileSizeRemaining);
			 * 
			 * // Represents the receiving buffer byte[] receiveBuffer = new
			 * byte[chunkSize];
			 * 
			 * // Variable used to read if there are remaining size left to read. int num;
			 * 
			 * System.out.println("Downloading "+fileName+" of size "+fileSizeRemaining);
			 * 
			 * while((num = clientIn.read(receiveBuffer))>0){
			 * 
			 * // Write the received bytes into the RandomAccessFile
			 * downloadFileFromServer.write(Arrays.copyOf(receiveBuffer, num));
			 * 
			 * // Reduce the file size left to read.. fileSizeRemaining -= num;
			 * 
			 * // Set the chunkSize again chunkSize = setChunkSize(fileSizeRemaining);
			 * receiveBuffer = new byte[chunkSize];
			 * 
			 * // If you're done then break if(fileSizeRemaining == 0){ break; } }
			 * System.out.println("File received!"); downloadFileFromServer.close();
			 * 
			 * } } } }
			 * 
			 * } catch (UnknownHostException e) { System.out.println("Socket:" +
			 * e.getMessage()); } catch (EOFException e) { System.out.println("EOF:" +
			 * e.getMessage()); } catch (IOException e) { System.out.println("readline:" +
			 * e.getMessage()); } } public static int setChunkSize(long fileSizeRemaining){
			 * 
			 * // Determine the chunkSize int chunkSize=1024*1024;
			 * 
			 * // If the file size remaining is less than the chunk size // then set the
			 * chunk size to be equal to the file size. if(fileSizeRemaining<chunkSize){
			 * chunkSize=(int) fileSizeRemaining; }
			 * 
			 * return chunkSize; }
			 */
		}
	}
}
