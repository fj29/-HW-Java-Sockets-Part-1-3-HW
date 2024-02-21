package Module4.Part3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
    int port = 3001;
    private List<ServerThread> clients = new ArrayList<ServerThread>();
    private boolean isGameActive = false;
    private int hiddenNumber;


    //UCID:FJ28
    //DATE:2/21/2024
    private void start(int port) {
        this.port = port;
        try (ServerSocket serverSocket = new ServerSocket(port);) {
            Socket incoming_client = null;
            System.out.println("Server is listening on port " + port);
            do {
                System.out.println("waiting for next client");
                if (incoming_client != null) {
                    System.out.println("Client connected");
                    ServerThread sClient = new ServerThread(incoming_client, this);

                    clients.add(sClient);
                    sClient.start();
                    incoming_client = null;

                }
            } while ((incoming_client = serverSocket.accept()) != null);
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            System.out.println("closing server socket");
        }
    }

    protected synchronized void disconnect(ServerThread client) {
        long id = client.getId();
        client.disconnect();
        broadcast("Disconnected", id);
    }
 
    //UCID:FJ28
    //DATE:2/21/2024
    private void startNumberGuesser() {
        isGameActive = true;
        hiddenNumber = (int) (Math.random() * 100);
        broadcast("Number guesser game has started! Guess a number between 0 and 100.", -1);
    }

    private void stopNumberGuesser() {
        isGameActive = false;
        broadcast("Number guesser game has stopped. Guesses will be treated as regular messages.", -1);
    }

    private void processGuess(String message, long clientId) {
        if (isGameActive && message.startsWith("/guess")) {
            int guessedNumber = Integer.parseInt(message.split(" ")[1]);

            if (guessedNumber == hiddenNumber) {
                broadcast("Congratulations! User[" + clientId + "] guessed the correct number " + guessedNumber + "!",
                        -1);
                stopNumberGuesser();
            } else {
                broadcast("User[" + clientId + "] guessed " + guessedNumber + " but it was not correct.", -1);
            }
        }
    }

    //UCID:FJ28
    //DATE:2/21/2024
    private void processCoinToss(String message, long clientId) {
        if (message.equalsIgnoreCase("/flip") || message.equalsIgnoreCase("/toss") || message.equalsIgnoreCase("/coin")) {
            String result = (Math.random() < 0.5) ? "Heads" : "Tails";
            broadcast("User[" + clientId + "] flipped a coin and got " + result + "!", -1);
        }
    }

    protected synchronized void broadcast(String message, long id) {
        if (processCommand(message, id)) {
            return;
        }

        message = String.format("User[%d]: %s", id, message);

        Iterator<ServerThread> it = clients.iterator();
        while (it.hasNext()) {
            ServerThread client = it.next();
            boolean wasSuccessful = client.send(message);
            if (!wasSuccessful) {
                System.out.println(String.format("Removing disconnected client[%s] from list", client.getId()));
                it.remove();
                broadcast("Disconnected", id);
            }
        }

        if (isGameActive) {
            processGuess(message, id);
        } else {
            processCoinToss(message, id);
        }
    }

    private boolean processCommand(String message, long clientId) {
        System.out.println("Checking command: " + message);
        if (message.equalsIgnoreCase("/disconnect")) {
            Iterator<ServerThread> it = clients.iterator();
            while (it.hasNext()) {
                ServerThread client = it.next();
                if (client.getId() == clientId) {
                    it.remove();
                    disconnect(client);
                    break;
                }
            }
            return true;
        } else if (message.equalsIgnoreCase("/start")) {
            startNumberGuesser();
            return true;
        } else if (message.equalsIgnoreCase("/stop")) {
            stopNumberGuesser();
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println("Starting Server");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
