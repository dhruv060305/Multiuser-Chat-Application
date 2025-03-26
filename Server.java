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
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            ch.sendMessage(message);
        }
    }

    public void shutdown() {
        done = true;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                for (ConnectionHandler ch : connections) {
                    ch.shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

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
                broadcast(nickname + " has joined the chat room");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/name")) {
                        String newNickname = message.substring(6).trim();
                        if (!newNickname.isEmpty()) {
                            broadcast(nickname + " changed their name to " + newNickname);
                            System.out.println(nickname + " changed their name to " + newNickname);
                            nickname = newNickname;
                            out.println("Nickname changed to " + nickname);
                        } else {
                            out.println("Invalid nickname. Please try again.");
                        }
                    } else if (message.equals("/quit")) {
                        broadcast(nickname + " has left the chat room");
                        System.out.println(nickname + " has left the chat room");
                        shutdown();
                        break;
                    } else {
                        broadcast(nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
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
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
