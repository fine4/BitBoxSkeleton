package unimelb.bitbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;

//import org.apache.commons.codec.binary.Base64;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.EVENT;
import unimelb.bitbox.util.FileSystemManager.FileDescriptor;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.FileSystemObserver;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	private Document responseInfo;
	protected FileSystemManager fileSystemManager;
	protected FileDescriptor fileDescriptor;
	private DataOutputStream serverOut;
	public Document responseDocument = new Document();
	Base64 encoder = new Base64();
	
	int bufferSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));		
	byte[] buffer = new byte[bufferSize];
	//public Document responseInfo = new Document();

	
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		//fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);

	}
	public ServerMain(DataOutputStream serverOut)throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
		this.serverOut = serverOut;

	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		
		if (fileSystemEvent.event == EVENT.FILE_CREATE) {
			responseInfo = new SystemEventMessage().fileCreateRequest(fileSystemEvent.fileDescriptor.toDoc(), fileSystemEvent.name);
			try {
				serverOut.writeUTF(responseInfo.toJson());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (fileSystemEvent.event == EVENT.FILE_DELETE) {
			responseInfo = new SystemEventMessage().fileDeleteRequest(fileSystemEvent.fileDescriptor.toDoc(), fileSystemEvent.name);
			try {
				serverOut.writeUTF(responseInfo.toJson());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if (fileSystemEvent.event == EVENT.FILE_MODIFY) {
			responseInfo = new SystemEventMessage().fileModifyRequest(fileSystemEvent.fileDescriptor.toDoc(), fileSystemEvent.name);
			try {
				serverOut.writeUTF(responseInfo.toJson());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if (fileSystemEvent.event == EVENT.DIRECTORY_CREATE) {
			responseInfo = new SystemEventMessage().directoryCreateRequest(fileSystemEvent);
			try {
				serverOut.writeUTF(responseInfo.toJson());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if (fileSystemEvent.event == EVENT.DIRECTORY_DELETE) {
			responseInfo = new SystemEventMessage().directoryDeleteRequest(fileSystemEvent);
			try {
				serverOut.writeUTF(responseInfo.toJson());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
	
	public void HandleFileSystemEvent (Document info,DataOutputStream serverOut,DataInputStream serverIn) throws NoSuchAlgorithmException, IOException {

		String value = info.getString("Command");
		  switch (value) {
		  case "FILE_CREATE_REQUEST":{
		   responseDocument = FileCreateResponse(info);		   
		   serverOut.writeUTF(responseDocument.toJson());
		   serverOut.flush();
		   responseDocument = new SystemEventMessage().fileBytesRequest(info);
		   serverOut.writeUTF(responseDocument.toJson());
		   serverOut.flush();
		  }break;
		  
		  case "FILE_BYTES_REQUEST":{
		   responseDocument = SendFileBuffer(info ,serverOut);
		   serverOut.writeUTF(responseDocument.toJson());
		   serverOut.flush();
		  }break;
		  
		  case "FILE_BYTES_RESPONSE":{
			ReceveiedFileBuffer(info); 
		  }break;
		 
		  case "FILE_MODIFY_REQUEST":{
		   Document fileDescriptorDoc = (Document) info.get("FileDescriptor");
		   String fileMd5 = fileDescriptorDoc.getString("md5");
		   long fileLastModified = fileDescriptorDoc.getLong("lastModified");
		   fileSystemManager.modifyFileLoader(info.getString("pathName"), fileMd5, fileLastModified);
		   
		  }break;
		  case "FILE_DELETE_REQUEST":{
		   Document fileDescriptorDoc = (Document) info.get("FileDescriptor");
		   String fileMd5 = fileDescriptorDoc.getString("md5");
		   long fileLastModified = fileDescriptorDoc.getLong("lastModified");
		   boolean result = fileSystemManager.deleteFile(info.getString("pathName"), fileLastModified, fileMd5);
		   serverOut.writeBoolean(result);
		   
		  }break;
		  case "DIRECTORY_CREATE_REQUEST":{
		   boolean result = fileSystemManager.makeDirectory(info.getString("pathName"));
		   serverOut.writeBoolean(result);
		   
		  }break;
		  case "DIRECTORY_DELETE_REQUEST":{
		   boolean result = fileSystemManager.deleteDirectory(info.getString("pathName"));
		   serverOut.writeBoolean(result);
		   
		  }break;
		  }
	}
	
	
	
	public Document FileCreateResponse (Document info) {
		Document tempInfo = new Document();
		Document fileDescriptor = (Document) info.get("FileDescriptor");
		if (fileSystemManager.isSafePathName(info.getString("pathName"))) {
			if(!fileSystemManager.fileNameExists(info.getString("pathName"))) {
				String fileMd5 = fileDescriptor.getString("md5");
				long fileLastModified = fileDescriptor.getLong("lastModified");
				long fileSize = fileDescriptor.getLong("fileSize");
				try {
					fileSystemManager.createFileLoader(info.getString("pathName"), fileMd5, fileSize, fileLastModified);
					try {
						if(!fileSystemManager.checkShortcut(info.getString("pathName"))) {
							tempInfo = new SystemEventMessage().fileCreateResponseSuccess(info);		
							return tempInfo;
						}
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}else {
				tempInfo = new SystemEventMessage().fileCreateResponseRefuseExist(info);
				return tempInfo;
			}
		}else {
			tempInfo = new SystemEventMessage().fileCreateResponseRefuse(fileDescriptor, info.getString("pathName"));
			return tempInfo;
		}
		return tempInfo;
	}
	


	public Document SendFileBuffer (Document info,DataOutputStream serverOut) throws NumberFormatException, NoSuchAlgorithmException, IOException {
		//get information needed to be sent.
		Document sendFileInfo = new Document();
		Document fileDescriptorDoc = (Document) info.get("FileDescriptor");
		String fileMd5 = fileDescriptorDoc.getString("md5");
		long position = info.getLong("position");
		long length = fileDescriptorDoc.getLong("fileSize");
		@SuppressWarnings("deprecation")
		int fileSize = new Long(fileDescriptorDoc.getLong("fileSize")).intValue();
		ByteBuffer revFile = ByteBuffer.allocate(fileSize);
		revFile = new ServerMain(serverOut).fileSystemManager.readFile(fileMd5, position, length);

		byte[] lastBuffer = new byte[4096];
		revFile.rewind();
		if(revFile.capacity() < bufferSize) {			
			responseInfo = convertBufferToBase64StringInfo(revFile, buffer, info);
			serverOut.writeUTF(responseInfo.toJson());
			serverOut.flush();
		}else {
			while (revFile.hasRemaining()) {
				if(revFile.remaining() < bufferSize) {
					revFile.get(lastBuffer, 0, revFile.remaining());
					buffer = Base64.encodeBase64(buffer);
					String base64EncodeInfo = new String(buffer);
					responseInfo = new SystemEventMessage().fileBytesResponse(info, base64EncodeInfo);
					serverOut.writeUTF(responseInfo.toJson());
					serverOut.flush();
				} else {
				revFile.get(buffer, 0, bufferSize);
				buffer = Base64.encodeBase64(buffer);
				String base64EncodeInfo = new String(buffer);
				responseInfo = new SystemEventMessage().fileBytesResponse(info, base64EncodeInfo);
				serverOut.writeUTF(responseInfo.toJson());
				serverOut.flush();	
				}
			}
			
		}
		
		

		
		return sendFileInfo;		
	}
	public Boolean ReceveiedFileBuffer(Document info) throws IOException, NumberFormatException, NoSuchAlgorithmException {
		Document fileDescriptor = new Document();
		fileDescriptor = (Document) info.get("fileDescriptor");
		long fileSize = fileDescriptor.getLong("fileSize");
		int fileSizeInt = new Long(fileSize).intValue();
		ByteBuffer tempStoreBuffer = ByteBuffer.allocate(fileSizeInt);
		String content = info.getString("content");
		byte[] buffer = Base64.decodeBase64(content.getBytes());
		ByteBuffer infoBytebuffer = ByteBuffer.wrap(buffer);
		long position = info.getLong("position");
		int bufferPosition = 0;
		
		boolean writeResult = fileSystemManager.writeFile(info.getString("pathName"),
				infoBytebuffer, position);
		
		if (writeResult) {
			boolean checkWriteComplete = fileSystemManager.checkWriteComplete(info.getString("pathName"));
			if(!checkWriteComplete) {
				tempStoreBuffer.put(buffer, bufferPosition, buffer.length);
				responseDocument = new SystemEventMessage().fileBytesRequest(info);
				serverOut.writeUTF(responseDocument.toJson());
				serverOut.flush();
				bufferPosition = buffer.length+1;
			}else {
				responseDocument = new SystemEventMessage().fileCreateResponseSuccess(info);
				serverOut.writeUTF(responseDocument.toJson());
				serverOut.flush();
			}
		}
		else {
			responseDocument = new SystemEventMessage().fileCreateResponseRefuseFail(info);
			serverOut.writeUTF(responseDocument.toJson());
			serverOut.flush();
		}	 

		return writeResult;
		
	}
	
	public Document convertBufferToBase64StringInfo (ByteBuffer revFile,byte[] buffer, Document info) {
		Document sendFileInfo = new Document();
		revFile.get(buffer, 0, revFile.remaining());
		buffer = Base64.encodeBase64(buffer);
		String base64EncodeInfo = new String(buffer);
		sendFileInfo = new SystemEventMessage().fileBytesResponse(info, base64EncodeInfo);
		return sendFileInfo;
		
	}
	
	public Document convertBase64StringToBuffer () {
		Document receivedFileInfo = new Document();
		return receivedFileInfo;
		
	}
}

