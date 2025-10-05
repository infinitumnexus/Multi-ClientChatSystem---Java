import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String name;
    private ConcurrentHashMap<String, PrintWriter> clients;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, PrintWriter> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // === LOGIN PROCESS ===
            out.println("ENTER_NAME");
            name = in.readLine();

            while (name == null || name.trim().isEmpty() || clients.containsKey(name)) {
                out.println("NAME_TAKEN");
                name = in.readLine();
            }

            clients.put(name, out);
            out.println("NAME_ACCEPTED");
            broadcast("SERVER: " + name + " has joined the chat");

            // === MESSAGE HANDLING LOOP ===
            String message;
            while ((message = in.readLine()) != null) {

                // 1️⃣ Ignore file commands
                if (message.startsWith("/sendfile ")) {
                    System.out.println("⚠ File command from " + name + " ignored in chat (handled by FileHandler)");
                    continue;
                }

                // 2️⃣ Handle private messages
                if (message.startsWith("/w ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length >= 3) {
                        String target = parts[1];
                        String privateMsg = parts[2];
                        PrintWriter targetOut = clients.get(target);

                        if (targetOut != null) {
                            targetOut.println("Private from " + name + ": " + privateMsg);
                            out.println("Private to " + target + ": " + privateMsg);
                        } else {
                            out.println("⚠ User " + target + " not found.");
                        }
                    } else {
                        out.println("⚠ Usage: /w <username> <message>");
                    }
                    continue;
                }

                // 3️⃣ Handle quit
                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }

                // 4️⃣ Broadcast normal chat
                broadcast(name + ": " + message);
            }
        } catch (IOException e) {
            System.out.println("⚠ Connection lost with " + name);
        } finally {
            if (name != null) {
                clients.remove(name);
                broadcast("SERVER: " + name + " has left the chat");
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Broadcast to all connected clients
    private void broadcast(String msg) {
        for (PrintWriter writer : clients.values()) {
            writer.println(msg);
        }
    }
}
