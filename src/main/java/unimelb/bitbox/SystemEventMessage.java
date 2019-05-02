package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

public class SystemEventMessage {
	
	public Document invalidProtocol() {
		Document info = new Document();
		info.append("Command", "INVALID_PROTOCOL");
		info.append("message", "message must contain a command field as string");
		return info;
	}
	public Document connectionRefused() {
		Document info = new Document();
		info.append("Command", "CONNECTION_REFUSED");
		info.append("message", "connection limit reached");
		info.append("peers", Configuration.getConfigurationValue("peers"));		
		return info;
	}
	
	public Document HandShakeRequest(String ipAddress, int port) {
		Document info = new Document();
		HostPort docHostPort = new HostPort(ipAddress, port);
		info.append("Command", "HANDSHAKE_REQUEST");
		info.append("HostPort", docHostPort.toDoc());
		return info;
	}
	public Document HandShakeResponse() {
		Document info = new Document();
		HostPort serverHostPort = new HostPort(Configuration.getConfigurationValue("advertisedName"),Integer.parseInt(Configuration.getConfigurationValue("port")));
		info.append("Command", "HANDSHAKE_RESPONSE");
		info.append("Hostport", serverHostPort.toDoc());
		return info;
	}
	public Document fileCreateRequest(Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_CREATE_REQUEST");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		return info;
	}
	
	public Document fileCreateResponseSuccess (Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_CREATE_RESPONSE");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		info.append("message", "file loader ready");
		info.append("status", true);
		return info;
	}
	public Document fileCreateResponseRefuseExist (Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_CREATE_RESPONSE");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		info.append("message", "pathname already exists");
		info.append("status", false);
		return info;
	}
	public Document fileCreateResponseRefuse (Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_CREATE_RESPONSE");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		info.append("message", "unsafe pathname given");
		info.append("status", false);
		return info;
	}
	
	public Document fileBytesRequest (Document info) {
		//long longInfo = info.getLong();
		Document fileDescriptorDoc = new Document();
		fileDescriptorDoc = (Document) info.get("FileDescriptor");
		info.append("Command", "FILE_BYTES_REQUEST");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", info.getString("pathName"));
		info.append("position", 0);
		info.append("length", Configuration.getConfigurationValue("blockSize"));
		return info;
	}
	
	public Document fileBytesResponse (Document info) {
		Document fileDescriptorDoc = new Document();
		fileDescriptorDoc = (Document) info.get("FileDescriptor");
		info.append("Command", "FILE_BYTES_REQUEST");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", info.getString("pathName"));
		info.append("position", 0);
		info.append("length", Configuration.getConfigurationValue("blockSize"));
		info.append("content", "aGVsbG8K");
		info.append("message", "successful read");
		info.append("status", true);
		return info;
	}
	
	public Document fileDeleteRequest (Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_DELETE_REQUEST");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		return info;
	}
	
	public Document fileDeleteResponseSuccess (Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_DELETE_RESPONSE");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		info.append("message", "file deleted");
		info.append("status", true);
		return info;
	}
	
	public Document fileDeleteResponseNotExist (Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_DELETE_RESPONSE");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		info.append("message", "there was a problem deleting the file");
		info.append("status", false);
		return info;
	}
	
	public Document fileModifyRequest (Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_MODIFY_REQUEST");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		return info;
	}
	
	public Document fileModifyReponseSuccess (Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_MODIFY_REQUEST");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		info.append("message", "file loader ready");
		info.append("status", true);
		return info;
	}
	
	public Document fileModifyReponseFail (Document fileDescriptorDoc, String fileName) {
		Document info = new Document();
		info.append("Command", "FILE_MODIFY_REQUEST");
		info.append("FileDescriptor", fileDescriptorDoc);
		info.append("pathName", fileName);
		info.append("message", "there was a problem modifying the file");
		info.append("status", false);
		return info;
	}
	
	public Document directoryCreateRequest (FileSystemEvent fileSystemEvent) {
		Document info = new Document();
		info.append("Command", "DIRECTORY_CREATE_REQUEST");
		info.append("pathName", fileSystemEvent.pathName);
		return info;
	}
	
	public Document directoryCreateReponseSuccess (FileSystemEvent fileSystemEvent) {
		Document info = new Document();
		info.append("Command", "DIRECTORY_CREATE_RESPONSE");
		info.append("pathName", fileSystemEvent.pathName);
		info.append("message", "directory created");
		info.append("status", true);
		return info;
	}
	
	public Document directoryCreateReponseFail (FileSystemEvent fileSystemEvent) {
		Document info = new Document();
		info.append("Command", "DIRECTORY_CREATE_RESPONSE");
		info.append("pathName", fileSystemEvent.pathName);
		info.append("message", "there was a problem creating the directory");
		info.append("status", false);
		return info;
	}
	
	public Document directoryDeleteRequest (FileSystemEvent fileSystemEvent) {
		Document info = new Document();
		info.append("Command", "DIRECTORY_DELETE_REQUEST");
		info.append("pathName", fileSystemEvent.pathName);
		return info;
	}
	
	public Document directoryDeleteReponseSuccess (FileSystemEvent fileSystemEvent) {
		Document info = new Document();
		info.append("Command", "DIRECTORY_DELETE_RESPONSE");
		info.append("pathName", fileSystemEvent.pathName);
		info.append("message", "directory deleted");
		info.append("status", true);
		return info;
	}
	
	public Document directoryDeleteReponseFail (FileSystemEvent fileSystemEvent) {
		Document info = new Document();
		info.append("Command", "DIRECTORY_DELETE_RESPONSE");
		info.append("pathName", fileSystemEvent.pathName);
		info.append("message", "there was a problem deleting the directory");
		info.append("status", false);
		return info;
	}

}
