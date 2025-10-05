import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int CHAT_PORT = 12345;
    private static final int FILE_PORT = 12346;
    private static ConcurrentHashMap<String, PrintWriter> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        new Thread(() -> startFileServer()).start(); // start file server thread

        try (ServerSocket serverSocket = new ServerSocket(CHAT_PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket, clients)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startFileServer() {
        try (ServerSocket fileServerSocket = new ServerSocket(FILE_PORT)) {
            while (true) {
                Socket fileSocket = fileServerSocket.accept();
                new Thread(new FileHandler(fileSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
