# 💬 Multi-Client Chat System with File Sharing

A Java-based real-time chat system that supports:
- Multiple concurrent clients
- Private messaging (`/msg <user> <message>`)
- File sharing between users (`/sendfile <user> <filepath>`)

---

   Features
- Multi-threaded server using `Socket` and `ServerSocket`
- Concurrent user management with `ConcurrentHashMap`
- Real-time communication via TCP sockets
- File transfer between clients
- Clean and modular Java code

---

   Technologies Used
- Java (Core + Networking)
- I/O Streams
- Multithreading

---

   How to Run

   1. Compile all files
```bash
javac ChatServer.java ClientHandler.java ChatClient.java FileHandler.java
