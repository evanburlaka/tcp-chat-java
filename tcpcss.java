// Evan Burlaka - TCP Chat Server
import java.util.*;
import java.io.*;
import java.net.*;

public class tcpcss {  // chat server
	public static void main(String[] args) {
		int port = 12345;  // default port
		if (args.length == 1) {
			port = Integer.parseInt(args[0]);  // use port from cmd line
		}
		List<ClientHandler> clientList = Collections.synchronizedList(new ArrayList<>());  // all clients
		int clientID = 0;  // ID used to name threads
		try {
			ServerSocket serverSock = new ServerSocket(port);
			System.out.println("Listener on port " + port);
			System.out.println("Waiting for connections...");
			while (true) {
				Socket sock = serverSock.accept();  // waiting for client
				System.out.println("New connection, thread name is Thread-" + clientID + ", ip is: " + sock.getInetAddress() + ", port: " + sock.getPort());
				System.out.println("Adding to list of sockets as " + clientID);
				ClientHandler handler = new ClientHandler(sock, clientID, clientList);
				clientList.add(handler);
				new Thread(handler, "Thread-" + clientID).start();  // new thread for client
				clientID++;
			}
		} catch (IOException e) {
			System.out.println("Server error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

class ClientHandler implements Runnable {  // handler for client messages
	private int clientID;
	private Socket clientSock;
	private List<ClientHandler> clientList;
	private String username;
	private BufferedReader in;
	private PrintWriter out;
	private static Map<String, String> pendingFileTransfers = new HashMap<>();
	
	ClientHandler(Socket sock, int id, List<ClientHandler> clients) {
		this.clientID = id;
		this.clientSock = sock;
		this.clientList = clients;
	}
	public void run() {
		try {
			in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));  // input
			out = new PrintWriter(clientSock.getOutputStream(), true);  // output
			username = in.readLine(); //client enters username
			sendToAll("[" + username + "] has joined the chat.");  // new user notification
			String message;
			while ((message = in.readLine()) != null) {  // check for specific commands
				if (message.equals("/quit")) {
					break;
				} else if (message.equals("/who")) {
					System.out.println("[" + username + "] requested online users list.");
					sendUserList();  // display who is connected
				} else if (message.startsWith("/sendfile ")) {  // "/sendfile user filename"
					String[] parts = message.split(" ", 3);  // splits command above into 3 parts
					if (parts.length == 3) {
						String targetUser = parts[1];
						String filename = parts[2];
						File file = new File(filename);
						long fileSizeKB = file.exists() ? file.length() / 1024 : 0;
						pendingFileTransfers.put(username,targetUser);
						forwardToUser(targetUser, "[File transfer initiated from " + username + " to " + targetUser + " " + filename + " (" + fileSizeKB + " KB)]");
						System.out.println("[File transfer initiated from " + username + " to " + targetUser + " " + filename + " (" + fileSizeKB + " KB)]");
					}
				} else if (message.startsWith("/acceptfile ")) {
					String[] parts = message.split(" ", 2);
					if (parts.length == 2) {
						String sender = parts[1];
						forwardToUser(sender, "[File transfer accepted from " + username + " to " + sender + "]");
						System.out.println("[File transfer accepted from " + username + " to " + sender + "]");
						System.out.println("Starting new file transfer thread, thread name is " + Thread.currentThread().getName());
						System.out.println("[Starting file transfer between " + sender + " and " + username + "]");
					}
				} else if (message.startsWith("/rejectfile ")) {
					String[] parts = message.split(" ", 2);
					if (parts.length == 2) {
						String sender = parts[1];
						forwardToUser(sender, "[File transfer rejected by " + username + " from " + sender + "]");
						System.out.println("[File transfer rejected by " + username + " from " + sender + "]");
					}
				} else if (message.startsWith("[PORT " )) {  // port number for file transfer
					String recipient = pendingFileTransfers.get(username);
					if (recipient != null) {
						forwardToUser(recipient, message);  // sends exact port line
						pendingFileTransfers.remove(username);  // clean up
						System.out.println("[File transfer complete from " + username + " to " + recipient + "]");
					}
				} else {
					sendToAll("[" + username + "] " + message);
				}
			}
		} catch (IOException e) {
			System.out.println("Handler Error with client " + clientID + ", error: " + e.getMessage());
		} finally {
			try {
				clientSock.close();
				clientList.remove(this);
				sendToAll("[" + username + "] has left the chat.");
			} catch (IOException e) {
				System.out.println("Closing socket error: " + e.getMessage());
			}
		}
	}
	private void sendToAll(String msg) {  // send message to all clients
		synchronized (clientList) {
			for (ClientHandler c : clientList) {
				if (c != this && c.out != null) {
					c.out.println(msg);  // print to each client
				}
			}
			System.out.println(msg);  // print to server console
		}
	}
	private void sendUserList() {  // display all users in chat
		StringBuilder sb = new StringBuilder("[Online users:");
		synchronized (clientList) {
			for (ClientHandler c : clientList) {
				if (c.username != null) {
					sb.append(" ").append(c.username);
				}
			}
		}
		sb.append("]");
		out.println(sb.toString());  // send to client
		System.out.println(sb.toString());  // for server console
	}
	private void forwardToUser(String targetUser, String msg) {  // send priv message to user
		synchronized (clientList) {
			for (ClientHandler c : clientList) {
				if (c.username.equals(targetUser)) {
					c.out.println(msg);
					break;
				}
			}
		}
	}
}