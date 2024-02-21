package Module4.Part3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends Thread {
    private Socket client;
    private boolean isRunning = false;
    private ObjectOutputStream out;// exposed here for send()
    private Server server;// ref to our server so we can call methods on it
    // more easily
    private boolean isNumberGuesserActive = false;

    private void info(String message) {
        System.out.println(String.format("Thread[%s]: %s", getId(), message));
    }

    public ServerThread(Socket myClient, Server server) {
        info("Thread created");
        // get communication channels to a single client
        this.client = myClient;
        this.server = server;
    }

    public void disconnect() {
        info("Thread being disconnected by server");
        isRunning = false;
        cleanup();
    }

    public boolean send(String message) {
        // added a boolean so we can see if the send was successful
        try {
            out.writeObject(message);
            return true;
        } catch (IOException e) {
            info("Error sending message to the client (most likely disconnected)");
            // comment this out to inspect the stack trace
            // e.printStackTrace();
            cleanup();
            return false;
        }
    }

    @Override
    public void run() {
        info("Thread starting");
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
            this.out = out;
            isRunning = true;
            String fromClient;
            while (isRunning && // flag to let us easily control the loop
                    (fromClient = (String) in.readObject()) != null // reads an object from the inputStream (null would
                                                                    // likely mean a disconnect)
            ) {

                info("Received from the client: " + fromClient);
                server.broadcast(fromClient, this.getId());

                if (isNumberGuesserActive) {
                    processNumberGuesserCommand(fromClient);
                } else {
                    processCoinTossCommand(fromClient);
                }
            } // close while loop
        } catch (Exception e) {
            // happens when the client disconnects
            e.printStackTrace();
            info("Client disconnected");
        } finally {
            isRunning = false;
            info("Exited thread loop. Cleaning up connection");
            cleanup();
        }
    }

    private void cleanup() {
        info("Thread cleanup() start");
        try {
            client.close();
        } catch (IOException e) {
            info("Client already closed");
        }
        info("Thread cleanup() complete");
    }


    //UNCID: FJ28
    //DATE: 2/21/24
    private void processNumberGuesserCommand(String command) {
        if (command.equalsIgnoreCase("/start")) {
            server.broadcast("Number guesser game has started! Guess a number between 0 and 100.", -1);
            isNumberGuesserActive = true;
        } else if (command.equalsIgnoreCase("/stop")) {
            server.broadcast("Number guesser game has stopped. Guesses will be treated as regular messages.", -1);
            isNumberGuesserActive = false;
        } else if (command.startsWith("/guess")) {
            int guessedNumber = Integer.parseInt(command.split(" ")[1]);

        }
    }

    private void processCoinTossCommand(String command) {
        if (command.equalsIgnoreCase("/flip") || command.equalsIgnoreCase("/toss") || command.equalsIgnoreCase("/coin")) {
            String result = (Math.random() < 0.5) ? "Heads" : "Tails";
            server.broadcast("User[" + getId() + "] flipped a coin and got " + result + "!", -1);
        }
    }
}
