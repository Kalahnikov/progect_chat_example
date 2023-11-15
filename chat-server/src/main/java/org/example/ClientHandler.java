package org.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

public class ClientHandler {
    private Socket socket;
    private Server server;
    private DataInputStream in;
    private DataOutputStream out;

    public void setUsername(String username) {
        this.username = username;
    }

    private String username;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        sendMessage("111");
        new Thread(() -> {
            try {
                sendMessage("222");
                socket.setSoTimeout(1200000);
                authenticateUser(server);
                communicateWithUser(server);
                sendMessage("333");
                disconnect();
            } catch (SocketTimeoutException e) {
                System.out.println("Пользователь " + username + " отключен за бездействие");
                sendMessage("Пользователь " + username + " отключен за бездействие");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
        sendMessage("444");
    }

    public void communicateWithUser(Server server) throws IOException {
        while (true) {
            // /exit -> disconnect()
            // /w user message -> user
            // /list -> getUserList
            // /kick user -> delete user

            String message = in.readUTF();
            if (message.startsWith("/")) {
                if (message.equals("/exit")) {
                    break;
                } else if (message.startsWith("/w")) {
                    server.broadcastMessageToUser(message);
                } else if (message.equals("/list")) {
                    List<String> userList = server.getUserList();
                    String joinedUsers =
                            String.join(", ", userList);
                    sendMessage("Server: " + joinedUsers);
                } else if (message.startsWith("/kick")) {
                    if (checkAdminRole()) {
                        server.kickUser(message);
                    } else {
                        sendMessage("Server: Вы не являетесь админом. Запрос не доступен");
                    }
                } else if (message.startsWith("/ban")) {
                    if (checkAdminRole()) {
                        sendMessage("Введите данные бана цифрами через пробел: дата и время с точностью до минут");
                        String change = in.readUTF();
                        server.banTimes(message, change);
                    } else {
                        sendMessage("Server: Вы не являетесь админом. Запрос не доступен");
                    }
                } else if (message.equals("/shutdown")) {
                    if (checkAdminRole()) {
                        sendMessage("Server остановлен");
                        server.stopClients();
                        server.end();
                    } else {
                        sendMessage("Server: Вы не являетесь админом. Запрос не доступен");
                    }
                } else if (message.equals("/changenick")) {
                    sendMessage("Введите Ваш текуший UserName и желаемый UserName");
                    String change = in.readUTF();
                    server.changeNick(change, this);
                }
            } else {
                server.broadcastMessage("Server: " + message);
            }
        }
    }

    public void authenticateUser(Server server) throws IOException {
        boolean isAuthenticated = false;
        while (!isAuthenticated) {
            String message = in.readUTF();
//            /auth login password
//            /register login nick password
            String[] args = message.split(" ");
            String command = args[0];
            switch (command) {
                case "/auth": {
                    dataBase data = new dataBase();
                    String login = args[1];
                    String password = args[2];
                    String username = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
                    Timestamp timestamp = data.setBanUserDate(username);
                    if (username == null || username.isBlank()) {
                        sendMessage("Указан неверный логин/пароль");
                    } else if (timestamp != null) {
                        if (timestamp.after(Timestamp.valueOf(LocalDateTime.now()))) {
                            sendMessage("Вы находитесь в бане");
                        } else {
                            this.username = username;
                            sendMessage("Пользователь " + username + ", добро пожаловать в чат!");
                            server.subscribe(this);
                            isAuthenticated = true;
                        }
                    } else {
                        this.username = username;
                        sendMessage("Пользователь " + username + ", добро пожаловать в чат!");
                        server.subscribe(this);
                        isAuthenticated = true;
                    }
                    String authWithRole = server.getAuthenticationProvider().getRoleByUserRole(args[1], args[2], "Admin1");
                    if (authWithRole != null) {
                        if (authWithRole.equals(ROLE.ADMIN.toString())) {
                            sendMessage("Доступные операции чата для админа:\n" +
                                    "личные сообщения: /w + nick\n" +
                                    "просмотр активных пользователей: /list\n" +
                                    "смена имени: /changenick\n" +
                                    "выйти из чата: /exit\n" +
                                    "отключение пользователя: /kick + userName\n" +
                                    "бан: /ban + userName\n" +
                                    "остановка сервера: /shutdown");
                        }
                    } else {
                        sendMessage("Доступные операции чата для пользователя:\n" +
                                "личные сообщения: /w + nick\n" +
                                "просмотр активных пользователей: /list\n" +
                                "смена имени: /changenick\n" +
                                "выйти из чата: /exit");
                    }
                    break;
                }
                case "/register": {
                    String login = args[1];
                    String nick = args[2];
                    String password = args[3];
                    boolean isRegistred = server.getAuthenticationProvider().register(login, password, nick);
                    if (!isRegistred) {
                        sendMessage("Указанный логин/никнейм уже заняты");
                    } else {
                        this.username = nick;
                        sendMessage("Пользователь " + nick + ", добро пожаловать в чат!");
                        server.subscribe(this);
                        isAuthenticated = true;
                        sendMessage("Доступные операции чата для пользователя:\n" +
                                "личные сообщения: /w + nick\n" +
                                "просмотр активных пользователей: /list\n" +
                                "смена имени: /changenick\n" +
                                "выйти из чата: /exit");
                    }
                    break;
                }
                default: {
                    sendMessage("Сначала авторизуйтесь");
                }
            }
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void shutdown() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessage(String message) {
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String time = formatter.format(new Date());
        try {
            out.writeUTF(message + " " + time + "\r\n");
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public boolean checkAdminRole() throws IOException {
        sendMessage("Введите свои логин, пароль и роль в чате");
        String examination = in.readUTF();
        String[] args = examination.split(" ", 3);
        if ((server.getAuthenticationProvider().getRoleByUserRole(args[0], args[1], args[2])).equals(ROLE.ADMIN.toString())) {
            return true;
        }
        return false;
    }

}
