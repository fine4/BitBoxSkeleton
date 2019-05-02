package unimelb.bitbox;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

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
	FileDescriptor fileDescriptor;
	private DataOutputStream serverOut;
	public Document responseDocument = new Document();
	ByteBuffer revFile = ByteBuffer.allocate(Integer.parseInt(Configuration.getConfigurationValue("blockSize")));
	//public Document responseInfo = new Document();

	
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);

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
	
	public void HandleFileSystemEvent (FileSystemManager fileSystemManager, Document info,DataOutputStream serverOut) throws NoSuchAlgorithmException, IOException {
		if (info.get("Command").equals("FILE_CREATE_REQUEST")) {
			responseDocument = FileCreateResponse(fileSystemManager, info);
			
			serverOut.writeUTF(responseDocument.toJson());
			responseDocument = new SystemEventMessage().fileBytesRequest(info);
			serverOut.writeUTF(responseDocument.toJson());
		}
		if (info.get("Command").equals("FILE_BYTES_REQUEST")) {
			
			Document fileDescriptorDoc = (Document) info.get("FileDescriptor");
			String fileMd5 = fileDescriptorDoc.getString("md5");
			long position = info.getLong("position");
			long length = (long) info.get("length");
			revFile = fileSystemManager.readFile(fileMd5, position, length);
		}
		
		if(info.get("Command").equals("FILE_MODIFY_REQUEST")) {
			Document fileDescriptorDoc = (Document) info.get("FileDescriptor");
			String fileMd5 = fileDescriptorDoc.getString("md5");
			long fileLastModified = fileDescriptorDoc.getLong("lastModified");
			fileSystemManager.modifyFileLoader(info.getString("pathName"), fileMd5, fileLastModified);
		}
		
		if (info.get("Command").equals("FILE_DELETE_REQUEST")) {
			Document fileDescriptorDoc = (Document) info.get("FileDescriptor");
			String fileMd5 = fileDescriptorDoc.getString("md5");
			long fileLastModified = fileDescriptorDoc.getLong("lastModified");
			boolean result = fileSystemManager.deleteFile(info.getString("pathName"), fileLastModified, fileMd5);
			serverOut.writeBoolean(result);
		}
		
		if(info.get("Command").equals("DIRECTORY_CREATE_REQUEST")) {
			boolean result = fileSystemManager.makeDirectory(info.getString("pathName"));
			serverOut.writeBoolean(result);
		}
		
		if(info.get("Command").equals("DIRECTORY_DELETE_REQUEST")) {
			boolean result = fileSystemManager.deleteDirectory(info.getString("pathName"));
			serverOut.writeBoolean(result);
		}
		

	}
	
	
	
	public Document FileCreateResponse (FileSystemManager fileSystemManager, Document info) {
		Document tempInfo = new Document();
		Document fileDescriptor = (Document) info.get("fileDescriptor");
		if (fileSystemManager.isSafePathName(info.getString("pathName"))) {
			if(fileSystemManager.fileNameExists(info.getString("pathName"))) {
				String fileMd5 = fileDescriptor.getString("md5");
				long fileLastModified = fileDescriptor.getLong("lastModified");
				long fileSize = fileDescriptor.getLong("fileSize");
				try {
					fileSystemManager.createFileLoader(info.getString("pathName"), fileMd5, fileSize, fileLastModified);
					try {
						if(!fileSystemManager.checkShortcut(info.getString("pathName"))) {
							tempInfo = new SystemEventMessage().fileCreateResponseSuccess(fileDescriptor, info.getString("pathName"));		
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
				tempInfo = new SystemEventMessage().fileCreateResponseRefuseExist(fileDescriptor, info.getString("pathName"));
				return tempInfo;
			}
		}else {
			tempInfo = new SystemEventMessage().fileCreateResponseRefuse(fileDescriptor, info.getString("pathName"));
			return tempInfo;
		}
		return tempInfo;
	}
	

}
