package ru.glushko.multiplatformChat;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread
{
    public static String _nickname;
    private static final Scanner _in = new Scanner(System.in);
    public static void main(String[] args)
    {
        String nicknameBuffer;
        String PORTBuffer;

        while(true)
        {
            System.out.print("Enter Nickname: ");
            nicknameBuffer = _in.nextLine();

            if (!nicknameBuffer.isEmpty())
            {
                _nickname = nicknameBuffer;
                break;
            }
            else
                System.out.println("Nickname is not correct!");
        }

        String socketPort;
        while (true)
        {
            System.out.print("Enter the port(1000 - 64000): ");
            PORTBuffer = _in.nextLine();

            if(!PORTBuffer.isEmpty() && PORTBuffer.length() > 3)
            {
                socketPort = PORTBuffer;
                break;
            }
        }

        try
        {
            String ipAddress = "90.188.47.57";
            Socket clientSocket = new Socket(ipAddress, Integer.parseInt(socketPort));
            if(clientSocket.isConnected())
                System.out.println("Welcome to server, " + _nickname);

            new Input(clientSocket);
            new Output(clientSocket, _nickname);

        }catch (Exception e)
        {
            System.out.println("\n\n\n\n");
            System.out.println("Адрес сервера или порт были введены неверно.");
            System.out.println("Попробуйте снова :з (после перезапуска). ");
        }

    }
}

class Output extends Thread
{
    private final Socket _clientSocket;
    private String _clientNickname;
    private PrintWriter _writer;
    private Scanner _in;

    public Output(Socket clientSocket, String clientNickname) throws IOException
    {
        _clientSocket = clientSocket;
        _clientNickname = clientNickname;
        _writer = new PrintWriter(new OutputStreamWriter(_clientSocket.getOutputStream()), true);
        _in = new Scanner(System.in);
        OutputThread.start();
    }

    Thread OutputThread = new Thread(() ->
    {
        _writer.println(_clientNickname);
        while (true)
        {
            try
            {
                String message = _in.nextLine();
                if (message.equals("/disconnect"))
                {
                    disconnectFromServer();
                    break;
                }
                sendMessage(message);
                Thread.sleep(135);
            } catch (Exception e) { e.printStackTrace(); }
        }
    });

    private void disconnectFromServer() throws InterruptedException, IOException
    {
        _writer.println("/disconnect");
        Thread.sleep(700);
        _clientSocket.close();
        _writer.close();
        interrupt();
    } //Метод отключения от сервера.

    private void sendMessage(String message)
    {
        _writer.println(message);
    } //Метод отправки сообщений.
}

class Input extends Thread
{
    private BufferedReader _reader;

    public Input(Socket clientSocket) throws IOException
    {
        _reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        InputThread.start();
    }

    Thread InputThread = new Thread(() ->
    {
        try
        {
            String clientMessage;
            while ((clientMessage = _reader.readLine()) != null)
            {
                System.out.println(clientMessage);
            }
        }catch (IOException e) { e.printStackTrace(); }
    });
}