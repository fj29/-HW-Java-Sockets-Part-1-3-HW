package Module4.Part3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    private final String ipAddressPattern = "connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})";
    private final String localhostPattern = "connect\\s+(localhost:\\d{3,5})";
    private boolean isRunning = false;
    private Thread inputThread;
    private Thread fromServerThread;

    public Client() {
        System.out.println("Client initialized");
    }

    public boolean isConnected() {
        return server != null && server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            System.out.println("Client connected");
            listenForServerMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    private boolean isConnection(String text) {
        return text.matches(ipAddressPattern) || text.matches(localhostPattern);
    }

    private boolean isQuit(String text) {
        return text.equalsIgnoreCase("quit");
    }

    private boolean processCommand(String text) {
        if (isConnection(text)) {
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            return true;
        } else if (isQuit(text)) {
            isRunning = false;
            return true;
        }
        return false;
    }

    private void listenForKeyboard() {
        inputThread = new Thread() {
            @Override
            public void run() {
                System.out.println("Listening for input");
                try (Scanner si = new Scanner(System.in)) {
                    String line;
                    isRunning = true;
                    while (isRunning) {
                        try {
                            System.out.println("Waiting for input");
                            line = si.nextLine();
                            if (!processCommand(line)) {
                                if (isConnected()) {
                                    out.writeObject(line);
                                } else {
                                    System.out.println("Not connected to server");
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Connection dropped");
                            break;
                        }
                    }
                    System.out.println("Exited loop");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close();
                }
            }
        };
        inputThread.start();
    }

    private void listenForServerMessage() {
        fromServerThread = new Thread() {
            @Override
            public void run() {
                try {
                    String fromServer;
                    while (!server.isClosed() && !server.isInputShutdown()
                            && (fromServer = (String) in.readObject()) != null) {
                        System.out.println(fromServer);
                    }
                    System.out.println("Loop exited");
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!server.isClosed()) {
                        System.out.println("Server closed connection");
                    } else {
                        System.out.println("Connection closed");
                    }
                } finally {
                    close();
                    System.out.println("Stopped listening to server input");
                }
            }
        };
        fromServerThread.start();
    }

    public void start() throws IOException {
        listenForKeyboard();
    }

    private void close() {
        try {
            inputThread.interrupt();
        } catch (Exception e) {
            System.out.println("Error interrupting input");
            e.printStackTrace();
        }
        try {
            fromServerThread.interrupt();
        } catch (Exception e) {
            System.out.println("Error interrupting listener");
            e.printStackTrace();
        }
        try {
            System.out.println("Closing output stream");
            if (out != null) {
                out.close();
            }
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened, so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Closing input stream");
            if (in != null) {
                in.close();
            }
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened, so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Closing connection");
            if (server != null) {
                server.close();
            }
            System.out.println("Closed socket");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened, so this exception is ok");
        }
    }

    //UCID: FJ28
    //DATE: 2/21/2024

    public void startNumberGuesser() throws IOException {
        if (isConnected()) {
            out.writeObject("/start");
        } else {
            System.out.println("Not connected to the server");
        }
    }

    public void stopNumberGuesser() throws IOException {
        if (isConnected()) {
            out.writeObject("/stop");
        } else {
            System.out.println("Not connected to the server");
        }
    }

    public void makeGuess(int guess) throws IOException {
        if (isConnected()) {
            out.writeObject("/guess " + guess);
        } else {
            System.out.println("Not connected to the server");
        }
    }

    public void flipCoin() throws IOException {
        if (isConnected()) {
            out.writeObject("/flip");
        } else {
            System.out.println("Not connected to the server");
        }
    }

    public static void main(String[] args) {
        Client client = new Client();

        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            client.startNumberGuesser();
            client.stopNumberGuesser();
            int guess = 5; // Enter the guess value here
            client.makeGuess(guess);
            client.flipCoin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
