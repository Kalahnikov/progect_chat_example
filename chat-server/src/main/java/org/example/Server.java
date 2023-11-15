package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private final AuthenticationProvider authenticationProvider;
    ServerSocket serverSocket;
    Socket socket;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
        clients = new ArrayList<>();
        this.authenticationProvider = authenticationProvider;
    }


    public void start() throws IOException {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Сервер запущен на порту " + port);
            while (true) {
                socket = serverSocket.accept();
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            serverSocket.close();
        }
    }

    public void end() {
        try {
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastMessage("Клиент: " + clientHandler.getUsername() + " вошел в чат");
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void broadcastMessageToUser(String message) {
        String[] msg = message.split(" ", 3);
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(msg[1])) {
                client.sendMessage(msg[2]);
            }
        }
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Клиент: " + clientHandler.getUsername() + " вышел из чата");
    }

    public synchronized List<String> getUserList() {
        return clients.stream().map(ClientHandler::getUsername).collect(Collectors.toList());
    }

    public synchronized void stopClients() {
        for (ClientHandler client : clients) {
            client.shutdown();
        }
    }

    public synchronized void kickUser(String message) {
        String[] msg = message.split(" ", 2);
        ClientHandler clientKick = null;
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(msg[1])) {
                clientKick = client;
            }
        }
        if (clientKick != null) {
            clientKick.disconnect();
        }
    }

    public synchronized void changeNick(String change, ClientHandler clientHandler) {
        String[] msg = change.split(" ", 2);
        dataBase datBase = new dataBase();
        datBase.setUpdateUsername(msg[0], msg[1]);
        clientHandler.setUsername(msg[1]);
    }

    public synchronized void banTimes(String message, String change) {
        dataBase datBase = new dataBase();
        datBase.setUpdateBanUser(message, change);
        kickUser(message);
    }
}
