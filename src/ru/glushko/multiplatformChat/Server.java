package ru.glushko.multiplatformChat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server
{
    private static ServerSocket _serverSocket;
    private static Socket _clientSocket;
    public static String _nicknameClient;
    public static ArrayList<PrintWriter> _clientsList = new ArrayList();
    private static PrintWriter _writer;
    private static Scanner _reader;
    private static Scanner _in = new Scanner(System.in);

    public static void main (String[] args) throws IOException
    {
        int PORT;
        while(true)
        {
            try{
                System.out.print("Enter the port(1000 - 64000): ");
                PORT = _in.nextInt();
                break;
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        _serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port: " + PORT);
        while (true)
        {
            try
            {
                _clientSocket = _serverSocket.accept();
                _reader = new Scanner(_clientSocket.getInputStream());
                _writer = new PrintWriter(new OutputStreamWriter(_clientSocket.getOutputStream()), true);
                _clientsList.add(_writer);
                if (_reader.hasNext())
                    _nicknameClient = _reader.nextLine();
                //_clientsName.add(_nicknameClient);
                System.out.println(_clientsList.toString());
                new MessageReceiver(_clientSocket, _nicknameClient, _writer);
                System.out.println("Подключился: " + _nicknameClient);
                MessageReceiver.broadcastMessage("\t\t" + "Подключился: " + _nicknameClient);

            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void removeClient(PrintWriter printWriter)
    {
        _clientsList.remove(printWriter);
        System.out.println(_clientsList.toString());
    }
}

class MessageReceiver extends Thread
{
    private Socket _clientSocket;
    private BufferedReader _reader;
    private static String _clientMessage;
    private PrintWriter _writer;
    public static String _nickname;
    private static Server _server = new Server();

    public MessageReceiver(Socket client, String nickname, PrintWriter writer) throws IOException
    {
        this._clientSocket = client;
        this._nickname = nickname;
        _reader = new BufferedReader(new InputStreamReader(_clientSocket.getInputStream()));
        this._writer = writer;
        reciver.start();
    }

    Thread reciver = new Thread(() ->
    {
        try
        {
            while ((_clientMessage = _reader.readLine()) != null)
            {
                if (_clientMessage.equals("/disconnect"))
                {
                    //broadcastMessage(_nickname + " disconnected!");
                    _server.removeClient(this._writer); //Удаление клиента из списка.
                    _clientSocket.close(); //Закрытие сокета.
                    _writer.close(); //Закрытие потока на запись.
                    _reader.close(); //Закрытие потока на чтение.
                    interrupt();
                    break; //Завершение цикла.
                } else
                {
                    broadcastMessage(_clientMessage); //Отправка сообщения всем пользователям.
                    System.out.println(_clientMessage);
                }

            }
        }catch(IOException e)
        {
            //e.printStackTrace(); //Печать ошибки в консоль.
        }finally
        {
            try
            {
                broadcastMessage(_nickname + " disconnected!");
                System.out.println(_nickname + " disconnected!");
                _server.removeClient(this._writer); //Удаление клиента из списка.
                _clientSocket.close(); //Закрытие сокета.
                _writer.close(); //Закрытие потока на запись.
                _reader.close(); //Закрытие потока на чтение.
                interrupt();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    });

    //public static void sendMessage(String message)
    //{
        //_writer.println(message);
    //}

    public static void broadcastMessage(String message)
    {
        Iterator iterator = Server._clientsList.iterator();
        while (iterator.hasNext())
        {
            PrintWriter printWriter = (PrintWriter) iterator.next();
            printWriter.println(message);
            printWriter.flush();
        }
    }
}
