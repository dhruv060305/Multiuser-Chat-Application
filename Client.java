import java.io.IOException;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;

public class Client implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;

    @Override
    public void run() {
        try {
            client = new Socket("192.168.29.146", 8080); // Fixed variable name from 'socket' to 'client'
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inputHandler = new InputHandler();
            Thread t = new Thread(inputHandler);
            t.start();

            String inMessage; // Fixed typo 'Srting' to 'String'
            while ((inMessage = in.readLine()) != null) { // Fixed parentheses placement
                System.out.println(inMessage);
            }
        } catch (IOException e) {
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
            // Ignore
        }
    }

    class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in)); // Fixed variable name from 'inReader' to 'input'
                while (!done) {
                    String message = input.readLine(); // Fixed variable name from 'inReader' to 'input'
                    if (message.equals("/quit")) {
                        input.close(); // Fixed variable name from 'inReader' to 'input'
                        shutdown();
                    } else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }
    }

    public static void main(String[] args) { // Fixed 'Public' to 'public'
        Client client = new Client();
        client.run();
    }
}