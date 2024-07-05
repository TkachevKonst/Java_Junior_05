package ru.gb.javajun;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.gb.javajun.jsonClass.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class Server {

    private final static ObjectMapper objectMapper = new ObjectMapper();


    public static void main(String[] args) {

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<String, ClientHandler> clients = new HashMap<>();
        try (ServerSocket server = new ServerSocket(8887);) {
            System.out.println("Сервер в работе");

            while (true) {
                System.out.println("Сервер ждет подключение новых клиентов");
                Socket client = server.accept();
                ClientHandler clientHandler = new ClientHandler(clients, client);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка во время работы сервера: " + e.getMessage());
        }
    }


    private static class ClientHandler implements Runnable {

        private final Socket client;
        private final Scanner in;
        private final PrintWriter out;
        private final Map<String, ClientHandler> clients;
        private String clientLogin;

        public ClientHandler(Map<String, ClientHandler> clients, Socket client) throws IOException {
            this.clients = clients;
            this.client = client;

            this.in = new Scanner(client.getInputStream());
            this.out = new PrintWriter(client.getOutputStream(), true);
        }


        @Override
        public void run() {
            System.out.println("Подключен новый клиент");
            try {
                String loginRequest = in.nextLine();
                LoginRequest request = objectMapper.reader().readValue(loginRequest, LoginRequest.class);
                this.clientLogin = request.getLogin();
            } catch (IOException e) {
                System.err.println("Не удалось прочитать сообщение от клиета [" + clientLogin + "]: " + e.getMessage());
                String unsuccessfulResponse = createLoginResponse(false);
                out.println(unsuccessfulResponse);
                doClose();
                return;
            }

            System.out.println("Запрос от клиента: " + clientLogin);
            if (clients.containsKey(clientLogin)) {
                String unsuccessfulResponse = createLoginResponse(false);
                out.println(unsuccessfulResponse);
                doClose();
                return;
            }
            clients.put(clientLogin, this);
            String successfulResponse = createLoginResponse(true);
            out.println(successfulResponse);


            while (true) {
                String msgFromClient = in.nextLine();
                final String type;
                try {
                    QueryType queryType = objectMapper.reader().readValue(msgFromClient, QueryType.class);
                    type = queryType.getType();
                } catch (IOException e) {
                    System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
                    sendMessage("Не удалось прочитать сообщение: " + e.getMessage());
                    continue;
                }
                if (SendMessageRequest.TYPE.equals(type)) {
                    final SendMessageRequest request;
                    try {
                        request = objectMapper.reader().readValue(msgFromClient, SendMessageRequest.class);
                    } catch (IOException e) {
                        System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
                        sendMessage("Не удалось прочитать сообщение SendMessageRequest: " + e.getMessage());
                        continue;
                    }
                    ClientHandler clientTo = clients.get(request.getRecipient());
                    if (clientTo == null) {
                        sendMessage("Клиент [" + request.getRecipient() + "] не найден");
                        continue;
                    }
                    clientTo.sendMessage(clientLogin + ": " + request.getMessage());
                } else if (BroadcastMessageRequest.TYPE.equals(type)) {
                    final BroadcastMessageRequest request;
                    try {
                        request = objectMapper.reader().readValue(msgFromClient, BroadcastMessageRequest.class);
                    } catch (IOException e) {
                        System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
                        sendMessage("Не удалось прочитать сообщение SendMessageRequest: " + e.getMessage());
                        continue;
                    }
                    for (Map.Entry<String, ClientHandler> clientHandler : clients.entrySet()) {
                        ClientHandler handler = clientHandler.getValue();
                        if (!handler.clientLogin.equals(clientLogin)) {
                            handler.sendMessage(clientLogin + ": " + request.getMessage());
                        }

                    }
                } else if (DisconnectRequest.TYPE.equals(type)) {
                    try {
                        objectMapper.reader().readValue(msgFromClient, DisconnectRequest.class);

                    } catch (IOException e) {
                        System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
                        sendMessage("Не удалось прочитать сообщение SendMessageRequest: " + e.getMessage());
                        continue;
                    }
                    for (Map.Entry<String, ClientHandler> clientHandler : clients.entrySet()) {
                        ClientHandler handler = clientHandler.getValue();
                        if (!handler.clientLogin.equals(clientLogin)) {
                            handler.sendMessage(clientLogin + ": вышел из сети!");
                        }
                        doClose();
                        clients.remove(clientLogin);


                    }
                } else if (UsersRequest.TYPE.equals(type)) {
                    try {
                        objectMapper.reader().readValue(msgFromClient, UsersRequest.class);
                    } catch (IOException e) {
                        System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
                        sendMessage("Не удалось прочитать сообщение SendMessageRequest: " + e.getMessage());
                        continue;
                    }
                    sendMessage("Список подключенных клиентов: ");
                    int count = 0;
                    for (Map.Entry<String, ClientHandler> clientHandler : clients.entrySet()) {
                        String login = clientHandler.getKey();
                        sendMessage(login);
                        count++;
                    }
                } else {
                    System.err.println("Неизвестный тип сообщения: " + type);
                    sendMessage("Неизвестный тип сообщения: " + type);
                }
            }
        }

        private String createLoginResponse(boolean success) {
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setConnecting(success);
            try {
                return objectMapper.writer().writeValueAsString(loginResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("не удалось создать loginResponse " + e.getMessage());
            }
        }

        private void doClose() {
            try {
                in.close();
                out.close();
                client.close();
            } catch (IOException e) {
                System.err.println("Ошибка во время отключения клиента: " + e.getMessage());
            }
        }

        private void sendMessage(String message) {
            out.println(message);
        }
    }
}
