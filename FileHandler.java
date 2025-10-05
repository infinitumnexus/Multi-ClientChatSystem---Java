import java.io.*;
import java.net.Socket;

public class FileHandler implements Runnable {
    private Socket fileSocket;

    public FileHandler(Socket socket) {
        this.fileSocket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(fileSocket.getInputStream())) {
            String sender = dis.readUTF();
            String receiver = dis.readUTF();
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();

            System.out.println("📩 File '" + fileName + "' from " + sender + " to " + receiver);

            // Re-broadcast to all connected file clients
            for (Socket client : FileRegistry.getClients()) {
                try {
                    DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                    dos.writeUTF(sender);
                    dos.writeUTF(receiver);
                    dos.writeUTF(fileName);
                    dos.writeLong(fileSize);

                    byte[] buffer = new byte[4096];
                    long totalRead = 0;
                    while (totalRead < fileSize) {
                        int read = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead));
                        if (read == -1) break;
                        dos.write(buffer, 0, read);
                        totalRead += read;
                    }
                    dos.flush();
                } catch (IOException e) {
                    System.out.println("⚠ Failed to send file to a client");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
