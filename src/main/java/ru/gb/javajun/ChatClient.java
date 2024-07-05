package ru.gb.javajun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.gb.javajun.jsonClass.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);


        try (Socket server = new Socket("localhost", 8887);) {
            String login = scanner.nextLine();
            try (PrintWriter out = new PrintWriter(server.getOutputStream(), true)) {
                Scanner in = new Scanner(server.getInputStream());

                String loginRequest = createLoginRequest(login);
                out.println(loginRequest);

                String loginResponsenString = in.nextLine();
                if (!checkLoginResponse(loginResponsenString)) {
                    System.out.println("Не удалось подключиться к серверу");
                    return;
                }
                System.out.println("Упешно подключились к серверу");
                new Thread(() -> {
                    while (true) {
                        String msgFromServer = in.nextLine();
                        System.out.println(msgFromServer);

                    }
                }).start();


                while (true) {
                    System.out.println("Меню");
                    System.out.println("1. Получить список логинов");
                    System.out.println("2. Послать сообщение всем");
                    System.out.println("3. Послать сообщение другу");
                    System.out.println("4. Выйти из чата");
                    String type = scanner.nextLine();
                    if (type.equals("1")) {
                        UsersRequest request = new UsersRequest();
                        String usersRequest = objectMapper.writeValueAsString(request);
                        out.println(usersRequest);
                    } else if (type.equals("2")) {
                        BroadcastMessageRequest request = new BroadcastMessageRequest();
                        request.setMessage(scanner.nextLine());
                        String broadcastMessageRequest = objectMapper.writeValueAsString(request);
                        out.println(broadcastMessageRequest);
                    } else if (type.equals("3")) {
                        SendMessageRequest request = new SendMessageRequest();
                        request.setMessage(scanner.nextLine());
                        request.setRecipient(scanner.nextLine());
                        String sendMsgRequest = objectMapper.writeValueAsString(request);
                        out.println(sendMsgRequest);
                    } else if (type.equals("4")) {
                        DisconnectRequest request = new DisconnectRequest();
                        String disconnectRequest = objectMapper.writeValueAsString(request);
                        out.println(disconnectRequest);
                        return;
                    } else System.out.println("Неверная команда");
                }

            }
        } catch (IOException e) {
            System.err.println("Ошибка во время подключения к серверу: " + e.getMessage());
        }
        System.out.println("Отключились от сервера");
    }


    private static String createLoginRequest(String login) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLogin(login);
        try {
            return objectMapper.writeValueAsString(loginRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка JSON: " + e.getMessage());
        }
    }

    private static boolean checkLoginResponse(String loginResponse) {
        try {
            LoginResponse response = objectMapper.reader().readValue(loginResponse, LoginResponse.class);
            return response.isConnecting();
        } catch (IOException e) {
            System.err.println("Ошибка чтения JSON: " + e.getMessage());
            return false;
        }
    }

}
