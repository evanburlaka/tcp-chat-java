// Evan Burlaka - TCP Chat Client
import java.io.*;
import java.net.*;

public class tcpccs {
	static class PendingFileTransfer {  // helper class for file transfer
		String ip = null;
		int port = -1;
	}
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: java tcpccs <server_host> <username>");
			return;
		}
		String serverHost = args[0];
		String username = args[1];
		try {
			Socket serverSock = new Socket(serverHost, 12345);  // server connection (chat)
			System.out.println("Connected to server. You can start sending messages.");
			PrintWriter out = new PrintWriter(serverSock.getOutputStream(), true);  // output to server
			BufferedReader in = new BufferedReader(new InputStreamReader(serverSock.getInputStream()));  // input from server
			BufferedReader inUser = new BufferedReader(new InputStreamReader(System.in));  // keyboard input from user
			out.println(username);  // send username
			PendingFileTransfer pending = new PendingFileTransfer();  // pending file transfer info
			Thread listener = new Thread(() -> {  // print messages (server)
				try {
					String serverMsg;
					while ((serverMsg = in.readLine()) != null) {
						if (serverMsg.startsWith("[PORT ") && serverMsg.contains(" IP ")) {
							try {
								String raw = serverMsg.substring(6, serverMsg.length() - 1);
								String[] parts = raw.split(" IP ");
								pending.port = Integer.parseInt(parts[0]);
								pending.ip = parts[1];
								//System.out.println("[Incoming file transfer request received]");
							} catch (Exception e) {
								System.out.println("Failed to parse port/ip message: " + e.getMessage());
							}
						} else {
							System.out.println(serverMsg);
						}
					}
				} catch (IOException e) {
					System.out.println("Disconnected, server closed");
				}
			});
			listener.start();
			while (true) {
				String userMsg = inUser.readLine();
				if (userMsg == null || userMsg.equals("/quit")) {
					out.println("/quit");
					break;
				}
				if (userMsg.startsWith("/acceptfile ")) {  // if accepting file, begin reciever thread
					String sender = userMsg.split(" ", 2)[1];
					if (pending.ip != null && pending.port != -1) {
						final String targetIP = pending.ip;
						final int targetPort = pending.port;
						pending.ip = null;  // clear before launching thread
						pending.port = -1;
						new Thread(() -> {
							try {
								Socket fileSock = new Socket(targetIP, targetPort);
								InputStream fileIn = fileSock.getInputStream();
								FileOutputStream fileOut = new FileOutputStream("received_file.txt");
								byte[] buffer = new byte[4096];
								int bytesRead;
								while ((bytesRead = fileIn.read(buffer)) != -1) {
									fileOut.write(buffer, 0, bytesRead);
								}
								fileOut.close();
								fileSock.close();
								System.out.println("[File transfer complete]");
							} catch (Exception e) {
								System.out.println("Error when receiving file: " + e.getMessage());
							}
						}).start();
					}
				}
				if (userMsg.startsWith("/sendfile ")) {  // if sending file, begin acceptor thread
					String[] parts = userMsg.split(" ", 3);
					if (parts.length == 3) {
						String target = parts[1];
						String fileName = parts[2];
						new Thread(() -> {
							try {
								ServerSocket fileServer = new ServerSocket(0);  // open port
								int port = fileServer.getLocalPort();
								String ip = serverSock.getLocalAddress().getHostAddress();
								PrintWriter notifyOut = new PrintWriter(serverSock.getOutputStream(), true);
								notifyOut.println("[PORT " + port + " IP " + ip + "]");
								System.out.println("[Starting file transfer on port " + port + "]");
								Socket sendSock = fileServer.accept();
								OutputStream fileOut = sendSock.getOutputStream();
								FileInputStream fileIn = new FileInputStream(fileName);
								byte[] buffer = new byte[4096];
								int bytesRead;
								while ((bytesRead = fileIn.read(buffer)) != -1) {
									fileOut.write(buffer, 0, bytesRead);
								}
								fileIn.close();
								sendSock.close();
								fileServer.close();
								System.out.println("[File transfer complete]");
							} catch (Exception e) {
								System.out.println("Error when sending file: " + e.getMessage());
							}
						}).start();
					}
				}
				if (userMsg.startsWith("/rejectfile ")) {  // if rejecting file, no thread needed
					String sender = userMsg.split(" ", 2)[1];
					System.out.println("[File transfer rejected]");
				}
				out.println(userMsg);
			}
			serverSock.close(); //cleanup before exit
			System.exit(0);
		} catch (IOException e) {
			System.out.println("Unable to connect, server may not be active: " + e.getMessage());
		}
	}
}