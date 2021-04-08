package ru.glushko.multiplatformChat;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Client extends Thread
{
    private static String _ipAddress = "90.188.47.57";
    public static String _nickname;
    private static Socket _clientSocket;
    private static String _port;
    private static final Scanner _in = new Scanner(System.in);
    public static void main(String[] args)
    {
        String nicknameBuffer;
        String ipAddressBuffer;
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

        /*while (true)
        {
            System.out.print("Enter IP Address: ");
            ipAddressBuffer = _in.nextLine();

            if (!ipAddressBuffer.isEmpty() && ipAddressBuffer.length() > 7)
            {
                _ipAddress = ipAddressBuffer;
                break;
            }
            else
                System.out.println("IPAddress is not correct!");
        }*/

        while (true)
        {
            System.out.print("Enter the port(1000 - 64000): ");
            PORTBuffer = _in.nextLine();

            if(!PORTBuffer.isEmpty() && PORTBuffer.length() > 3)
            {
                _port = PORTBuffer;
                break;
            }
        }

        try
        {
            _clientSocket = new Socket(_ipAddress, Integer.parseInt(_port));
            if(_clientSocket.isConnected())
                System.out.println("Welcome to server, " + _nickname);
            new Output(_clientSocket, _nickname);
            new Input(_clientSocket);
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
    private Socket _socket;
    private String _nickname;
    private PrintWriter _writer;
    private Scanner _in;

    public Output(Socket socket, String nickname) throws IOException
    {
        this._socket = socket;
        this._nickname = nickname;
        _writer = new PrintWriter(new OutputStreamWriter(_socket.getOutputStream()), true);
        _in = new Scanner(System.in);
        output.start();
    }

    Thread output = new Thread(() ->
    {
        Date date = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("hh:mm:ss");
        _writer.println(_nickname);
        while (true)
        {
            try
            {
                String message = _in.nextLine();
                if (message.equals("/disconnect"))
                {
                    _writer.println("/disconnect");
                    Thread.sleep(1000);
                    interrupt();
                    break;
                }
                _writer.println(formatForDateNow.format(date) + "/" + _nickname + ": " + message);
                Thread.sleep(135);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    });
}

class Input extends Thread
{
    private Socket _socket;
    private BufferedReader _reader;
    private String _clientMessage;

    public Input(Socket socket) throws IOException
    {
        this._socket = socket;
        _reader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
        input.start();
    }

    Thread input = new Thread(() ->
    {
        try
        {
            while ((_clientMessage = _reader.readLine()) != null )
            {
                System.out.println(_clientMessage);
            }
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    });
}

