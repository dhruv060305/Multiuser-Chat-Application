import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket serverSocket;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(8080);
            pool = Executors.newCachedThreadPool();
            System.out.println("Server started on port 8080...");
            while (!done) {
                Socket socket = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(socket);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public synchronized void broadcast(String message, ConnectionHandler excludeUser) {
        for (ConnectionHandler ch : connections) {
            if (ch != excludeUser) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        done = true;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            System.err.println("Error shutting down server: " + e.getMessage());
        }
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private volatile String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Hello, welcome to the server!");
                out.println("Enter Your Nickname:");
                nickname = in.readLine();
                
                if (nickname == null || nickname.trim().isEmpty()) {
                    shutdown();
                    return;
                }
                nickname = nickname.trim();
                out.println(nickname + " connected successfully!");
                broadcast(nickname + " has joined the chat room", this);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/name")) {
                        String newNickname = message.substring(6).trim();
                        if (!newNickname.isEmpty()) {
                            synchronized (connections) {
                                broadcast(nickname + " changed their name to " + newNickname, this);
                                System.out.println(nickname + " changed their name to " + newNickname);
                                nickname = newNickname;
                            }
                            out.println("Nickname changed to " + nickname);
                        } else {
                            out.println("Invalid nickname. Please try again.");
                        }
                    } else if (message.equals("/quit")) {
                        broadcast(nickname + " has left the chat room", this);
                        System.out.println(nickname + " has left the chat room");
                        shutdown();
                        break;
                    } else {
                        broadcast(nickname + ": " + message, this);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection error: " + e.getMessage());
            } finally {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        public void shutdown() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (client != null && !client.isClosed()) client.close();
                synchronized (connections) {
                    connections.remove(this);
                }
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
