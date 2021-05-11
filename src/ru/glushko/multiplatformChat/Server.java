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
    public static void main (String[] args) throws IOException
    {
        int PORT;
        while(true)
        {
            try{
                System.out.print("Enter the port(1000 - 64000): ");
                PORT = _in.nextInt();
                break;
            }catch (Exception e) { e.printStackTrace(); }
        }

        ServerSocket _serverSocket = new ServerSocket(PORT);//Создание серверного сокета.
        System.out.println("Server started on port: " + PORT);

        while (true)
        {
            try
            {
                Socket _clientSocket = _serverSocket.accept(); //Создание клиентского сокета и начало ожидания подключений.
                BufferedReader _reader = new BufferedReader(new InputStreamReader(_clientSocket.getInputStream()));
                PrintWriter _writer = new PrintWriter(new OutputStreamWriter(_clientSocket.getOutputStream()), true);
                _clientNickname = _reader.readLine(); //Получение никнейма пользователя.
                //TODO: Добавить проверку на существующий никнейм и проверку на пустой никнейм.
                _clientsList.put(_clientNickname, _writer); //Добавление пользователя и поток записи в список.
                showUsers(); //Метод отображения списка подключившихся пользователей.

                new UsersCommander(_clientSocket, _clientNickname, _writer); //Создание экземляра управляющего класса и запуск потока обработки сообщений.
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
        broadcastMessageToAllClients(_clientNickname + " joined the server!");
        Thread.sleep(135);
        broadcastMessageToAllClients("\n");
        broadcastMessageToAllClients("CLIENTS LIST: ");
        for (String name : _clientNicknames)
        {
            broadcastMessageToAllClients(name.trim());
            Thread.sleep(135);
        }
        broadcastMessageToAllClients("\n");
    } //Метод отображения списка подключившихся пользователей.

    public static void removeClient(String nickname, PrintWriter writer)
    {
        _clientsList.remove(nickname, writer);
    } //Метод удаления из списка подключившихся пользователей.
}

class UsersCommander extends Thread
{
    private final Socket _commanderClientSocket;
    private BufferedReader _commanderReader;
    private PrintWriter _commanderWriter;
    private String _clientNickname;

    public UsersCommander(Socket clientSocket, String clientNickname, PrintWriter serverWriter) throws IOException
    {
        _commanderClientSocket = clientSocket;
        _clientNickname = clientNickname;
        _commanderReader = new BufferedReader(new InputStreamReader(_commanderClientSocket.getInputStream()));
        _commanderWriter = serverWriter;
        ReceiverThread.start();
    }

    Thread ReceiverThread = new Thread(() -> {
        try
        {
            Date date = new Date();
            SimpleDateFormat formatForDateNow = new SimpleDateFormat("hh:mm:ss");
            String _clientMessage;
            while ((_clientMessage = _commanderReader.readLine()) != null)
            {
                if (_clientMessage.equals("/disconnect"))
                {
                    interrupt();
                    break; //Завершение цикла.
                }
                Server.broadcastMessageByWriterFilter("\r" + formatForDateNow.format(date) + "/" + _clientNickname + ": " + _clientMessage, _commanderWriter); //Отправка сообщения всем пользователям.
                Thread.sleep(135);
                System.out.println(formatForDateNow.format(date) + "/" + _clientNickname + ": " + _clientMessage);
            }
        }catch(IOException | InterruptedException e) { e.printStackTrace(); }finally { try { disconnectFromServer(); } catch (InterruptedException | IOException e) { e.printStackTrace(); } }
    });

    private void disconnectFromServer() throws InterruptedException, IOException
    {
        Server.broadcastMessageToAllClients(_clientNickname + " disconnected.");
        System.out.println(_clientNickname + " disconnected.");
        Server.removeClient(_clientNickname, _commanderWriter); //Удаление клиента из списка.
        if(!_commanderClientSocket.isClosed()) //Проверка на закрытый сокет.
            _commanderClientSocket.close(); //Закрытие сокета.
        _commanderWriter.close(); //Закрытие потока на запись.
        _commanderReader.close(); //Закрытие потока на чтение.
        if(ReceiverThread.isAlive()) //Проверка на жизнь потока.
            interrupt();
    } //Метод отключения от сервера.
}