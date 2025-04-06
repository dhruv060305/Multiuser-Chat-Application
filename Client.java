import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;

    @Override
    public void run() {
        try {
            client = new Socket("192.168.29.146", 8080);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            Thread inputThread = new Thread(new InputHandler());
            inputThread.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                System.out.println(inMessage);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            shutdown();
        }
    }

    public void shutdown() {
        done = true;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    class InputHandler implements Runnable {
        @Override
        public void run() {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in))) {
                while (!done) {
                    String message = input.readLine();
                    if (message.equals("/quit")) {
                        out.println(message);
                        shutdown();
                        break;
                    } else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Input error: " + e.getMessage());
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
