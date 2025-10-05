import java.io.*;
import java.net.Socket;

public class ChatClient {
    private Socket chatSocket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader console;
    private String username;
    private String serverAddress;
    private int chatPort = 12345;
    private int filePort = 12346;

    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        try {
            // Connect to chat server
            chatSocket = new Socket(serverAddress, chatPort);
            in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
            out = new PrintWriter(chatSocket.getOutputStream(), true);
            console = new BufferedReader(new InputStreamReader(System.in));

            // Start thread to listen for chat messages
            new Thread(new IncomingReader()).start();

            // Handle login
            while (true) {
                String serverMsg = in.readLine();
                if (serverMsg == null) break;

                System.out.println(serverMsg);

                if (serverMsg.equals("ENTER_NAME")) {
                    username = console.readLine();
                    out.println(username);
                } else if (serverMsg.equals("NAME_ACCEPTED")) {
                    System.out.println("✅ Connected as " + username);
                    break;
                }
            }

            // Start file listener
            new Thread(new FileListener()).start();

            // Handle user input
            while (true) {
                String message = console.readLine();
                if (message == null) break;

                if (message.equalsIgnoreCase("/quit")) {
                    out.println("/quit");
                    break;
                }

                // Intercept /sendfile BEFORE sending to chat
                if (message.startsWith("/sendfile ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length < 3) {
                        System.out.println("Usage: /sendfile <username> <filepath>");
                        continue;
                    }
                    sendFile(parts[1], parts[2]);
                    continue; // prevent sending raw command to server
                }

                // Normal chat
                out.println(message);
            }

            chatSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Listen for chat messages
    private class IncomingReader implements Runnable {
        @Override
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
            }
        }
    }

    // Listen for incoming files
    private class FileListener implements Runnable {
        @Override
        public void run() {
            try (Socket fileSocket = new Socket(serverAddress, filePort);
                 DataInputStream dis = new DataInputStream(fileSocket.getInputStream())) {

                while (true) {
                    String sender = dis.readUTF();
                    String receiver = dis.readUTF();
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();

                    if (!receiver.equals(username)) continue;

                    System.out.println("📂 File incoming: " + fileName + " (" + fileSize + " bytes) from " + sender);
                    System.out.print("Save as (full path): ");
                    String savePath = console.readLine();

                    try (FileOutputStream fos = new FileOutputStream(savePath)) {
                        byte[] buffer = new byte[4096];
                        long totalRead = 0;
                        while (totalRead < fileSize) {
                            int read = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead));
                            if (read == -1) break;
                            fos.write(buffer, 0, read);
                            totalRead += read;
                        }
                        fos.flush();
                        System.out.println("✅ Saved: " + savePath);
                    }
                }
            } catch (IOException e) {
                System.out.println("File listener stopped.");
            }
        }
    }

    // Send file
    private void sendFile(String receiver, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("❌ File not found: " + filePath);
            return;
        }

        try (Socket fileSocket = new Socket(serverAddress, filePort);
             DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {

            dos.writeUTF(username);
            dos.writeUTF(receiver);
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }

            System.out.println("📤 Sent '" + file.getName() + "' to " + receiver);
        } catch (IOException e) {
            System.out.println("❌ File send failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new ChatClient("127.0.0.1");
    }
}
