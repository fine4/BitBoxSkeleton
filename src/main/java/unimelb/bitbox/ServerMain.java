package unimelb.bitbox;

import java.io.BufferedWriter;
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
	protected static FileSystemManager fileSystemManager;
	protected FileDescriptor fileDescriptor;
	private BufferedWriter serverOut;
	public Document responseDocument = new Document();
	Base64 encoder = new Base64();

	int bufferSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
	

	public ServerMain(BufferedWriter serverOut) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		if (fileSystemManager == null)
			fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
		this.serverOut = serverOut;

	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {

		if (fileSystemEvent.event == EVENT.FILE_CREATE) {
			responseInfo = new SystemEventMessage().fileCreateRequest(fileSystemEvent.fileDescriptor.toDoc(),
					fileSystemEvent.name);
			try {
				serverOut.write(responseInfo.toJson());				
				serverOut.newLine();
				serverOut.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (fileSystemEvent.event == EVENT.FILE_DELETE) {
			responseInfo = new SystemEventMessage().fileDeleteRequest(fileSystemEvent.fileDescriptor.toDoc(),
					fileSystemEvent.name);
			try {
				serverOut.write(responseInfo.toJson());
				serverOut.newLine();
				serverOut.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if (fileSystemEvent.event == EVENT.FILE_MODIFY) {
			responseInfo = new SystemEventMessage().fileModifyRequest(fileSystemEvent.fileDescriptor.toDoc(),
					fileSystemEvent.name);
			try {
				serverOut.write(responseInfo.toJson());
				serverOut.newLine();
				serverOut.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if (fileSystemEvent.event == EVENT.DIRECTORY_CREATE) {
			responseInfo = new SystemEventMessage().directoryCreateRequest(fileSystemEvent);
			try {
				serverOut.write(responseInfo.toJson());
				serverOut.newLine();
				serverOut.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if (fileSystemEvent.event == EVENT.DIRECTORY_DELETE) {
			responseInfo = new SystemEventMessage().directoryDeleteRequest(fileSystemEvent);
			try {
				serverOut.write(responseInfo.toJson());
				serverOut.newLine();
				serverOut.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	public void HandleFileSystemEvent(Document info, BufferedWriter serverOut)
			throws NoSuchAlgorithmException, IOException {

		String value = info.getString("command");
		switch (value) {
		case "FILE_CREATE_REQUEST": {
			responseDocument = FileCreateResponse(info);
			serverOut.write(responseDocument.toJson());
			serverOut.newLine();
			serverOut.flush();
			if (responseDocument.getBoolean("status")) {
				responseDocument = new SystemEventMessage().fileBytesRequest(info);
				serverOut.write(responseDocument.toJson());
				serverOut.newLine();
				serverOut.flush();
			}
		}
			break;

		case "FILE_BYTES_REQUEST": {
			SendFileBuffer(info, serverOut);
		}
			break;

		case "FILE_BYTES_RESPONSE": {
			ReceveiedFileBuffer(info);

		}
			break;

		case "FILE_MODIFY_REQUEST": {
			Document fileDescriptorDoc = (Document) info.get("fileDescriptor");
			String fileMd5 = fileDescriptorDoc.getString("md5");
			long fileLastModified = fileDescriptorDoc.getLong("lastModified");
			if (!fileSystemManager.isSafePathName(info.getString("pathName"))) {
				responseDocument = new SystemEventMessage().fileModifyReponseUnsafePathname(fileDescriptorDoc,
						info.getString("pathName"));
				serverOut.write(responseDocument.toJson());
				serverOut.newLine();
				serverOut.flush();
			} else {
				if (!fileSystemManager.modifyFileLoader(info.getString("pathName"), fileMd5, fileLastModified)) {
					responseDocument = new SystemEventMessage().fileModifyReponseFail(fileDescriptorDoc,
							info.getString("pathName"));
					serverOut.write(responseDocument.toJson());
					serverOut.newLine();
					serverOut.flush();
				} else {
					responseDocument = new SystemEventMessage().fileModifyReponseSuccess(fileDescriptorDoc,
							info.getString("pathName"));
					serverOut.write(responseDocument.toJson());
					serverOut.newLine();
					serverOut.flush();
					if(responseDocument.getBoolean("status")) {
						responseDocument = new SystemEventMessage().fileBytesRequest(info);
						serverOut.write(responseDocument.toJson());
						serverOut.newLine();
						serverOut.flush();
					}
					
				}
			}
		}
			break;
		case "FILE_MODIFY_RESPONSE":{
			if (info.getBoolean("status") == true) {
				Document fileDescriptorDoc = (Document) info.get("fileDescriptor");
				responseDocument = new SystemEventMessage().fileModifyReponseSuccess(fileDescriptorDoc, info.getString("pathName"));
				serverOut.write(responseDocument.toJson());
				serverOut.newLine();
				serverOut.flush();
			}if((info.getBoolean("status") == true)) {
				Document fileDescriptorDoc = (Document) info.get("fileDescriptor");
				responseDocument = new SystemEventMessage().fileModifyReponseFail(fileDescriptorDoc, info.getString("pathName"));
				serverOut.write(responseDocument.toJson());
				serverOut.newLine();
				serverOut.flush();
			}

		}break;

		case "FILE_DELETE_REQUEST": {
			Document fileDescriptorDoc = (Document) info.get("fileDescriptor");
			String fileMd5 = fileDescriptorDoc.getString("md5");
			long fileLastModified = fileDescriptorDoc.getLong("lastModified");
			fileSystemManager.deleteFile(info.getString("pathName"), fileLastModified, fileMd5);
			responseDocument = new SystemEventMessage().fileDeleteRequest(fileDescriptorDoc,info.getString("pathName"));
			serverOut.write(responseDocument.toJson());
			serverOut.newLine();
			serverOut.flush();
		}
			break;
		case "DIRECTORY_CREATE_REQUEST": { // need to modify
			fileSystemManager.makeDirectory(info.getString("pathName"));
			responseDocument = new SystemEventMessage().directoryCreateReponseSuccess(info);
			serverOut.write(responseDocument.toJson());
			serverOut.newLine();
			serverOut.flush();

		}
			break;
		case "DIRECTORY_DELETE_REQUEST": {// need to modify
			fileSystemManager.deleteDirectory(info.getString("pathName"));	
			responseDocument = new SystemEventMessage().directoryCreateReponseSuccess(info);
			serverOut.write(responseDocument.toJson());
			serverOut.newLine();
			serverOut.flush();
		}
			break;
		}
	}

	public Document FileCreateResponse(Document info) {
		Document tempInfo = new Document();
		Document fileDescriptor = (Document) info.get("fileDescriptor");
		if (fileSystemManager.isSafePathName(info.getString("pathName"))) {
			if (!fileSystemManager.fileNameExists(info.getString("pathName"))) {
				String fileMd5 = fileDescriptor.getString("md5");
				long fileLastModified = fileDescriptor.getLong("lastModified");
				long fileSize = fileDescriptor.getLong("fileSize");
				try {
					fileSystemManager.createFileLoader(info.getString("pathName"), fileMd5, fileSize, fileLastModified);
					try {
						if (!fileSystemManager.checkShortcut(info.getString("pathName"))) {
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

			} else {
				tempInfo = new SystemEventMessage().fileCreateResponseRefuseExist(info);
				return tempInfo;
			}
		} else {
			tempInfo = new SystemEventMessage().fileCreateResponseRefuse(fileDescriptor, info.getString("pathName"));
			return tempInfo;
		}
		return tempInfo;
	}

	public void SendFileBuffer(Document info, BufferedWriter serverOut)
			throws NumberFormatException, NoSuchAlgorithmException, IOException {
		// get information needed to be sent.		
		Document fileDescriptorDoc = (Document) info.get("fileDescriptor");
		String fileMd5 = fileDescriptorDoc.getString("md5");
		long position = info.getLong("position");
		long length = fileDescriptorDoc.getLong("fileSize");
		@SuppressWarnings("deprecation")
		int fileSize = new Long(fileDescriptorDoc.getLong("fileSize")).intValue();
		ByteBuffer revFile = ByteBuffer.allocate(fileSize);
		
		revFile = fileSystemManager.readFile(fileMd5, position, length);
		revFile.rewind();
		
		if (revFile.capacity() < bufferSize) {
			byte[] buffer = new byte[bufferSize];
			responseInfo = convertBufferToBase64StringInfo(revFile, buffer, info);
			serverOut.write(responseInfo.toJson());
			serverOut.newLine();
			serverOut.flush();
		} else {
			while (revFile.hasRemaining()) {
				if (revFile.remaining() < bufferSize) {
					String base64EncodeInfo = new String();
					byte[] lastReaminBuffer = new byte[revFile.remaining()];
					revFile.get(lastReaminBuffer, 0, revFile.remaining());
					lastReaminBuffer = Base64.encodeBase64(lastReaminBuffer);
					base64EncodeInfo = new String(lastReaminBuffer);
					responseInfo = new SystemEventMessage().fileBytesResponse(info, base64EncodeInfo);
					serverOut.write(responseInfo.toJson());
					serverOut.newLine();
					serverOut.flush();
					info.append("length", revFile.remaining());
					info.append("position", revFile.position());
				} else {
					byte[] buffer = new byte[bufferSize];
					String base64EncodeInfo = new String();
					revFile.get(buffer, 0, bufferSize);
					buffer = Base64.encodeBase64(buffer);
					base64EncodeInfo = new String(buffer);
					responseInfo = new SystemEventMessage().fileBytesResponse(info, base64EncodeInfo);
					serverOut.write(responseInfo.toJson());
					serverOut.newLine();
					serverOut.flush();
					info.append("length", revFile.remaining());
					info.append("position", revFile.position());
				
				}

			}

		}

	}
	
	public void ReceveiedFileBuffer(Document info) throws IOException, NumberFormatException, NoSuchAlgorithmException {
		String content = info.getString("content");
		byte[] buffer = Base64.decodeBase64(content.getBytes());
		ByteBuffer infoBytebuffer = ByteBuffer.wrap(buffer);
		infoBytebuffer.rewind();
		long position = info.getLong("position");
		boolean checkWriteComplete = fileSystemManager.checkWriteComplete(info.getString("pathName"));
		if (!checkWriteComplete) {
			fileSystemManager.writeFile(info.getString("pathName"), infoBytebuffer, position);
			if (!fileSystemManager.checkWriteComplete(info.getString("pathName"))) {
				serverOut.flush();
			}
		}

	}

	public Document convertBufferToBase64StringInfo(ByteBuffer revFile, byte[] buffer, Document info) {
		Document sendFileInfo = new Document();
		byte[] lastReaminBuffer = new byte[revFile.remaining()];
		revFile.get(lastReaminBuffer, 0, revFile.remaining());
		lastReaminBuffer = Base64.encodeBase64(lastReaminBuffer);
		String base64EncodeInfo = new String(lastReaminBuffer);
		info.append("length", revFile.remaining());
		sendFileInfo = new SystemEventMessage().fileBytesResponse(info, base64EncodeInfo);
		return sendFileInfo;

	}

	public Document convertBase64StringToBuffer() {
		Document receivedFileInfo = new Document();
		return receivedFileInfo;

	}
}