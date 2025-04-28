## TCP Chat Client and Server (Java)

Multi-threaded TCP chat system developed in Java 11+, featuring real-time messaging and basic file transfer between clients.


## Files Included

 `tcpcss.java` → TCP Chat Server
 
 `tcpccs.java` → TCP Chat Client


## How to Run the Server


`java tcpcss.java`

The server will start listening on port `12345`.

Server console will display:
- Connected clients
- Incoming messages
- File transfer activity


## How to Run a Chat Client


`java tcpccs.java <server_hostname> <username>`


Example usage:

- `java tcpccs.java localhost bob`
- `java tcpccs.java localhost alice`

   - Establishes a connection to the server.
   - Displays incoming chat messages.
   - Allows sending chat messages and file transfer commands via console input.


## Available Client Commands


`/sendfile <username> <filename>`  -> Initiate a file transfer request to another user. 

`/acceptfile <username>`  -> Accept an incoming file transfer request. 

`/rejectfile <username>`  -> Decline an incoming file transfer request. 

`/who`  -> Display a list of all currently connected users. 

`/quit`  -> Disconnect gracefully from the chat server. 


## File Transfers

- Files are saved as `received_file.txt` in the receiving client's directory.
- Transfers occur over a separate TCP socket to avoid interrupting the main chat traffic.
- The sender will see `[File transfer complete ...]` if the file was successfully transferred.


## Testing Notes

- Each chat client should be run in a separate terminal session.
- If sending and receiving the same file, consider using separate folders for each client session.


## Development Notes

- Developed using only the Java SDK — no external libraries or IDEs were used.
- Java 11 Single Source Executables (SSE) format was utilized for simplicity.
- Tested entirely from the command line.


## Acknowledgment

Developed as a Computer Networking Fundamentals course project.  
Spring 2025 — California State University, Sacramento

