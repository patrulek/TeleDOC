package com.pp.iwm.teledoc.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.pp.iwm.teledoc.objects.FileTree;
import com.pp.iwm.teledoc.objects.ImageManager;
import com.pp.iwm.teledoc.objects.Member;
import com.pp.iwm.teledoc.objects.TempImage;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Point2D;

public class User extends Listener {
	
	// =======================================
	// FIELDS
	// =======================================

	private State state;
	
	public enum State {
		CONNECTING, RECONNECTING, CONNECTED, RECONNECTED, DISCONNECTED, CONNECTION_FAILURE
	}

	private String name;
	private String surname;
	private String email;
	
	private NetworkClient client;
	
	private List<Member> members;
	private MembersListListener members_listener;
	private DownloadListener download_listener;
	private FileTree file_tree;
	private FileTreeListener file_tree_listener;
	private String uploading_file_path;
	private TempImage downloading_file;
	private String downloading_file_path;
	private Map<Integer, Integer> used_images;		// file ids > image manager ids
	
	// TODO aktywna konferencja
	
	private static User user;
	private NetworkListener listener;
	private int current_image;
	
	
	// =======================================
	// METHODS
	// =======================================
	
	@Override
	public void connected(Connection _connection) {
		changeState(State.CONNECTED);
		System.out.println(_connection + " - connected");
		//super.connected(_connection);
	}
	
	@Override
	public void disconnected(Connection _connection) {
		changeState(State.DISCONNECTED);
		System.out.println(_connection + " - disconnected");
	}
	
	@Override
	public void received(Connection _connection, Object _message) {
		if( listener != null )
			listener.onReceive(_connection, _message);
		//super.received(_connection, _message);
	}
	
	public void changeState(State _new_state) {
		if( state == _new_state )
			return;
		
		state = _new_state;
		
		if( listener != null )
			listener.onStateChanged(state);
	}
	
	public State getState() {
		return state;
	}
	
	public static User instance() {
		if( user == null )
			user = new User();
		
		return user;
	}
	
	private User() {
		state = State.DISCONNECTED;
		file_tree = new FileTree();
		client = new NetworkClient();
		downloading_file = null;
		uploading_file_path = downloading_file_path = null;
		members = new ArrayList<>();
		used_images = new HashMap<>();
	}
	
	public void setDownloadListener(DownloadListener _listener) {
		download_listener = _listener;
	}
	
	public void removeDownloadListener() {
		download_listener = null;
	}
	
	public void setFileTreeListener(FileTreeListener _listener) {
		file_tree_listener = _listener;
	}
	
	public void removeFileTreeListener() {
		file_tree_listener = null;
	}
	
	public void setMembersListListener(MembersListListener _listener) {
		members_listener = _listener;
	}
	
	public void removeMembersListListener() {
		members_listener = null;
	}
	
	public void setListener(NetworkListener _listener) {
		listener = _listener;
	}

	public void removeListener() {
		listener = null;
	}
	
	public void connectToServer() {
		if( state != State.CONNECTION_FAILURE && state != State.DISCONNECTED )
			return;
		
		client.setUserAsListener();
		changeState(State.CONNECTING);
		
		Thread t = new Thread(() -> tryToConnect());
		t.start();
	}
	
	public void disconnectFromServer() {
		client.disconnectFromServer();
	}
	
	private void tryToConnect() {
		try {
			client.connectToServer();
		} catch (IOException _ex) {
			changeState(State.CONNECTION_FAILURE);
		}
	}
	
	public void reconnectToServer() {
		if( state != State.CONNECTION_FAILURE && state != State.DISCONNECTED )
			return;
		
		changeState(State.RECONNECTING);
		tryToReconnect();
	}
	
	private void tryToReconnect() {
		try {
			client.connectToServer();
		} catch (IOException _ex) {
			changeState(State.CONNECTION_FAILURE);
		}
	}
	
	public boolean isConnected() {
		return client.isConnected();
	}
	
	public void loadDataFromDB() {
		loadFileTreeFromDB();
		loadConferencesFromDB();
	}
	
	public void logIn(String _email, String _password) {
		client.sendLoginRequest(_email, _password);
	}
	
	public void register(String _name, String _surname, String _email, String _password) {
		client.sendRegisterRequest(_name, _surname, _email, _password);
	}
	
	private void loadConferencesFromDB() {
		client.sendLoadConferencesRequest(email);
	}
	
	public void createNewConference(String _conference_name) {
		client.sendNewConferenceRequest(email, _conference_name);
	}
	
	public void joinToConference(String _conference_name) {
		client.sendJoinToGroupRequest(email, _conference_name);
	}
	
	public void leaveConference() {
		client.sendLeaveGroupRequest(email);
	}
	
	public void logOut() {
		client.sendLogoutRequest(email);
	}
	
	public void sendChatMessage(String _message) {
		client.sendGroupMessageRequest(email, _message);
	}
	
	public void sendMousePos(Point2D _mouse_pos) {
		client.sendDispersedActionRequest(email, _mouse_pos);
	}

	public void sendPointerChanged(BooleanProperty is_switched_on) {
		client.sendDispersedActionRequest(email, is_switched_on);
	}
	
	public void createFolder(String _folder_name) {
		client.sendImageRequest(email, file_tree.getCurrentFolder().getPath() + _folder_name, null);
	}
	
	public void sendImage(File _image) {
		if( uploading_file_path == null ) {
			uploading_file_path = file_tree.getCurrentFolder().getPath() + _image.getName();
			client.sendImageRequest(email, file_tree.getCurrentFolder().getPath(), _image);
		} else 
			JOptionPane.showMessageDialog(null, "Obecnie trwa wysy�anie pliku: " + uploading_file_path);
	}
	
	public void downloadFile(String _filepath) {
		if( downloading_file_path == null ) {
			downloading_file_path = _filepath;
			client.downloadImageRequest(email, _filepath);
		} else
			JOptionPane.showMessageDialog(null, "Poczekaj a� poprzedni plik si� pobierze: " + downloading_file_path);
	}
	
	public void newDownloadingFile(int _size) {
		downloading_file = new TempImage(_size); 
		notifyDownloadListener(1);
	}
	
	public void progressDownload(byte[] _data) {
		downloading_file.appendData(_data);
		notifyDownloadListener(2);
	}
	
	public void saveFileToDisk(String _path, int _image_id) {
		try {				
			System.out.println("Zapisujemy w: " + _path);
			FileOutputStream imageOutFile = new FileOutputStream(_path);
			imageOutFile.write(downloading_file.getContent());
			imageOutFile.close();		
			ImageManager.instance().loadImageForUser(_path);
			addUsedImage(_image_id, ImageManager.instance().getLastLoadedImageId());
		} catch (Exception e) { e.printStackTrace(); } 
		  finally { 
			  downloading_file = null; 
			  downloading_file_path = null;
		  }	
		
		notifyDownloadListener(3);
	}
	
	public void getAllGroupMembers() {
		client.sendGetAllGroupMembersRequest(email);
	}
	
	// updateConferences()
	// removeNotExistingConferences()
	// addNewConferences()
	
	private void loadFileTreeFromDB() {
		client.sendGetAllImagesDescriptionRequest(email);
	}
	
	public void addUploadedFileToTree() {
		file_tree.addFile(uploading_file_path);
		uploading_file_path = null;
		
		notifyFileTreeListener();
	}

	public void addFilesToTree(List<String> _list_of_filepaths) {
		for( String path : _list_of_filepaths )
			file_tree.addFile(path);
		
		notifyFileTreeListener();
	}
	
	public void removeFileFromTree(String _filepath) {
		file_tree.removeFile(_filepath);
		
		notifyFileTreeListener();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String _name) {
		name = _name;
	}
	
	public String getSurname() {
		return surname;
	}
	
	public void setSurname(String _surname) {
		surname = _surname;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String _email) {
		email = _email;
		client.setUsername(_email);
	}
	
	public interface NetworkListener {
		public void onStateChanged(State _state);
		public void onReceive(Connection _connection, Object _message);
	}
	
	public interface FileTreeListener {
		public void onFileTreeChanged(FileTree _file_tree);
	}
	
	public interface MembersListListener {
		public void onMembersListChanged(Member _member, boolean is_removing);
	}
	
	public interface DownloadListener {
		public void onDownloadBegin();
		public void onDownloadProgress();
		public void onDownloadFinish();
	}
	
	public void addUsedImage(Integer _db_id, Integer _image_key) {
		used_images.put(_db_id, _image_key);
	}
	
	public void removeUsedImage(Integer _db_id) {
		used_images.remove(_db_id);
	}
	
	public void removeUsedImages() {
		used_images.clear();
	}
	
	public Map<Integer, Integer> getUsedImages() {
		return used_images;
	}
	
	public boolean isMemberInCurrentConference(String _member_mail) {
		return findMemberInCurrentConference(_member_mail) != null;
	}
	
	public Member findMemberInCurrentConference(String _member_mail) {
		for( Member m : members ) 
			if( m.email.equals(_member_mail) )
				return m;
		
		return null;
	}
	
	public void addMember(Member _member) {
		members.add(_member);
		notifyMembersListListener(_member, false);
	}
	
	public void removeMember(Member _member) {
		members.remove(_member);
		notifyMembersListListener(_member, true);
	}
	
	public void removeMembers() {
		members.clear();
		notifyMembersListListener(null, true);
	}
	
	public void setCurrentImage(int _current_image) {
		current_image = _current_image;
	}
	
	public int getCurrentImage() {
		return current_image;
	}

	public FileTree getFileTree() {
		return file_tree;
	}
	
	private void notifyFileTreeListener() {
		if( file_tree_listener != null )
			file_tree_listener.onFileTreeChanged(file_tree);
	}
	
	private void notifyMembersListListener(Member _member, boolean _is_removing) {
		if( members_listener != null )
			members_listener.onMembersListChanged(_member, _is_removing);
	}
	
	// TODO
	private void notifyDownloadListener(int _state) {
		if( download_listener != null ) {
			switch( _state ) {
				case 1:
					download_listener.onDownloadBegin();
					break;
				case 2:
					download_listener.onDownloadProgress();
					break;
				case 3:
					download_listener.onDownloadFinish();
					break;
			}
		}
	}
}
