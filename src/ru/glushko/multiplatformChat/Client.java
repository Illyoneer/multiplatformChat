package ru.glushko.multiplatformChat;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread
{
    public static String _nickname;
    private static String _socketPort;
    private final static String _ipAddress = "90.188.47.57";
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
                System.out.println("Nickname is not correct! Try again.");
        }

        while (true)
        {
            System.out.print("Enter the port(1000 - 64000): ");
            PORTBuffer = _in.nextLine();

            if(!PORTBuffer.isEmpty() && PORTBuffer.length() > 3 && PORTBuffer.length() < 6)
            {
                _socketPort = PORTBuffer;
                break;
            }
            else
                System.out.println("Port is not correct! Try again.");
        }

        try
        {
            Socket clientSocket = new Socket(_ipAddress, Integer.parseInt(_socketPort));
            if(clientSocket.isConnected())
                System.out.println("\b\b\b" + "SYSTEM: Welcome to server, " + _nickname);

            new Input(clientSocket);
            new Output(clientSocket, _nickname);

        }catch (Exception e)
        {
            System.out.println("       ");
            System.out.println("       ");
            System.out.println("       ");
            System.out.println("       ");
            System.out.println("\b\b\b" + "SYSTEM: Адрес сервера или порт были введены неверно.");
            System.out.println("\b\b\b" + "Попробуйте снова :з (после перезапуска). ");
        }

    }
}

class Output extends Thread
{
    private String _clientNickname;
    private PrintWriter _printWriter;
    private Scanner _in;


    public Output(Socket clientSocket, String clientNickname) throws IOException
    {
        _clientNickname = clientNickname;
        _printWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
        _in = new Scanner(System.in);
        OutputThread.start();
    }

    Thread OutputThread = new Thread(() ->
    {
        _printWriter.println(_clientNickname);
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
                if(message.length() > 0)
                    sendMessage(message);
                else
                    System.out.println("\b\b\b" + "SYSTEM: Enter a message!");
                Thread.sleep(135);
            } catch (Exception e) { e.printStackTrace(); }
            System.out.print(">: ");
        }
    });

    public void disconnectFromServer() throws InterruptedException
    {
        _printWriter.println("/disconnect");
        Thread.sleep(700);
        System.out.println("\b\b\b" + "SYSTEM: You were disconnected from the server.");
        System.exit(0);
    } //Метод отключения от сервера.

    private synchronized void sendMessage(String message)
    {
        _printWriter.println(message);
    } //Метод отправки сообщений.
}

class Input extends Thread
{
    private BufferedReader _bufferedReader;

    public Input(Socket clientSocket) throws IOException
    {
        _bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        InputThread.start();
    }

    Thread InputThread = new Thread(() ->
    {
        try
        {
            String clientMessage;
            while ((clientMessage = _bufferedReader.readLine()) != null)
            {
                System.out.println("\b\b\b" + clientMessage);
                System.out.print(">: ");
            }
        }catch (IOException e) { try { disconnectFromServer(); } catch (InterruptedException | IOException interruptedException) { interruptedException.printStackTrace(); }
        }
    });

    private void disconnectFromServer() throws InterruptedException, IOException
    {
        System.out.println("\b\b\b" + "SYSTEM: The server was shut down.");
        System.exit(0);
    } //Метод отключения от сервера.
}