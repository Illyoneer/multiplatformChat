package ru.glushko.multiplatformChat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server
{
    private static String _clientNickname;
    private static final Scanner _in = new Scanner(System.in);
    private static final HashMap<String, PrintWriter> _clientsList = new HashMap<>();
    private static final Set<String> _clientNicknames = _clientsList.keySet();

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws IOException
    {
        int PORT;
        while (true)
        {
            try
            {
                System.out.print("Enter the port(1000 - 64000): ");
                PORT = _in.nextInt();
                break;
            } catch (Exception e) { e.printStackTrace(); }
        }

        ServerSocket serverSocket = new ServerSocket(PORT);//Создание серверного сокета.
        System.out.println("Server started on port: " + PORT + ".");

        while (true)
        {
            try
            {
                Socket clientSocket = serverSocket.accept(); //Создание клиентского сокета и начало ожидания подключений.
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                _clientNickname = bufferedReader.readLine(); //Получение никнейма пользователя.
                //TODO: Добавить проверку на существующий никнейм.
                _clientsList.put(_clientNickname, printWriter); //Добавление пользователя и поток записи в список.
                showUsers(); //Метод отображения списка подключившихся пользователей.

                new UsersManipulator(clientSocket, _clientNickname, printWriter); //Создание экземляра управляющего классом и запуск потока обработки сообщений.
                System.out.println(_clientNickname + " joined the server!"); //Отладочная информация в консоль сервера.

            } catch (IOException | InterruptedException e) { e.printStackTrace(); }
        }
    }

    public static synchronized void broadcastMessageToAllClients(String message) throws InterruptedException
    {
        for (PrintWriter printWriter : _clientsList.values())
        {
            printWriter.println(message);
            printWriter.flush();
            Thread.sleep(150);
        }
    } //Метод отправки сообщений всем пользователям.

    public static synchronized void broadcastMessageByWriterFilter(String message, PrintWriter currentWriter) throws InterruptedException
    {
        for (PrintWriter printWriter : _clientsList.values())
        {
            if (printWriter != currentWriter)
            {
                printWriter.println(message);
                printWriter.flush();
                Thread.sleep(150);
            }

        }
    } //Метод отправки сообщений всем пользователям не являющихся отправителем.

    public static synchronized void showUsers() throws InterruptedException
    {
        broadcastMessageToAllClients(_clientNickname + " connected to the server!");
        Thread.sleep(135);
        broadcastMessageToAllClients("       ");
        broadcastMessageToAllClients("CLIENTS LIST: ");
        for (String name : _clientNicknames)
        {
            broadcastMessageToAllClients(name.trim());
        }
        broadcastMessageToAllClients("       ");
    } //Метод отображения списка подключившихся пользователей.

    public static void removeClient(String nickname, PrintWriter writer)
    {
        _clientsList.remove(nickname, writer);
    } //Метод удаления из списка подключившихся пользователей.
}

class UsersManipulator extends Thread
{
    private final Socket _commanderClientSocket;
    private BufferedReader _commanderBufferedReader;
    private PrintWriter _commanderPrintWriter;
    private String _clientNickname;

    public UsersManipulator(Socket clientSocket, String clientNickname, PrintWriter serverWriter) throws IOException
    {
        _commanderClientSocket = clientSocket;
        _clientNickname = clientNickname;
        _commanderBufferedReader = new BufferedReader(new InputStreamReader(_commanderClientSocket.getInputStream()));
        _commanderPrintWriter = serverWriter;
        MessageForwarderThread.start();
    }

    Thread MessageForwarderThread = new Thread(() ->
    {
        try
        {
            Date date = new Date();
            SimpleDateFormat formatForDateNow = new SimpleDateFormat("hh:mm:ss");
            String clientMessage;
            while ((clientMessage = _commanderBufferedReader.readLine()) != null)
            {
                if (clientMessage.equals("/disconnect"))
                {
                    interrupt();
                    break; //Завершение цикла.
                }
                if (clientMessage.length() > 0)
                    Server.broadcastMessageByWriterFilter(formatForDateNow.format(date) + "/" + _clientNickname + ": " + clientMessage,
                            _commanderPrintWriter); //Отправка сообщения всем пользователям кроме отправителя.
                Thread.sleep(135);
                System.out.println(formatForDateNow.format(date) + "/" + _clientNickname + ": " + clientMessage);
            }
        } catch (IOException | InterruptedException e) { e.printStackTrace(); }
        finally { try { disconnectFromServer(); } catch (InterruptedException | IOException e) { e.printStackTrace(); } }
    });

    private void disconnectFromServer() throws InterruptedException, IOException
    {
        Server.broadcastMessageToAllClients(_clientNickname + " disconnected.");
        System.out.println(_clientNickname + " disconnected.");
        Server.removeClient(_clientNickname, _commanderPrintWriter); //Удаление клиента из списка.
        if (!_commanderClientSocket.isClosed()) //Проверка на закрытый сокет.
            _commanderClientSocket.close(); //Закрытие сокета.
        _commanderPrintWriter.close(); //Закрытие потока на запись.
        _commanderBufferedReader.close(); //Закрытие потока на чтение.
        if (MessageForwarderThread.isAlive()) //Проверка на жизнь потока.
            interrupt();
    } //Метод отключения от сервера.
}