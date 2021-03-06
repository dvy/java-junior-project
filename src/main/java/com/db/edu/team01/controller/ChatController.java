package com.db.edu.team01.controller;

import com.db.edu.team01.decorator.Decorator;
import com.db.edu.team01.save.Saver;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatController {
    private static final String CMD_SEND = "/snd";
    private static final String CMD_HISTORY = "/hist";
    private static final String CMD_IDENTIFY = "/chid";
    private static final String CMD_EXIT = "/exit";

    private static final ArrayList<DataOutputStream> outStreams = new ArrayList<>();
    private static final Object addStreamMonitor = new Object();
    private static final Object sendMessageMonitor = new Object();
    private static final Object deleteOutputMonitor = new Object();

    private final DataOutputStream output;
    private final String responseSeparator = "&sep&";
    private final Saver fileSaver;
    private final Socket connection;
    private String userName;

    public ChatController(DataOutputStream output, Socket connection) {
        this.output = output;
        this.connection = connection;
        this.userName = null;

        fileSaver = new Saver("messageBase");
        addMonitor(output);
    }

    public void handleInput(String input) throws IOException {
        Message message = parseMessage(input);
        if (message.payloadIsValid()) {
            System.out.println("Valid message");
            sendCommand(message);
        } else {
            System.out.println("Invalid message");
            output.writeUTF("Input should be less than 150 symbols.");
            output.flush();
        }
    }

    Message parseMessage(String msg) {
        String[] input = msg.split(" ", 2);
        String payload;

        String command = input[0];
        if (input.length > 1) {
            payload = input[1];
        } else {
            payload = "";
        }

        return new Message(command, payload);
    }

    void sendCommand(Message message) throws IOException {
        switch (message.getCommand()) {
            case CMD_SEND:
                System.out.println("Sending your message...");
                sendMessage(message.getPayload());
                break;
            case CMD_HISTORY:
                System.out.println("Getting chat history...");
                getHistory();
                break;
            case CMD_IDENTIFY:
                System.out.println("Identifying...");
                setUserName(message.getPayload());
                break;
            case CMD_EXIT:
                System.out.println("Closing connection...");
                output.writeUTF("Closing connection");
                output.flush();
                deleteOutput(output);
                connection.close();
                break;
            default:
                output.writeUTF("Unknown command, try again");
                output.flush();
                break;
        }
    }

    private void sendMessage(String msg) throws IOException {
        if (userName == null) {
            try {
                output.writeUTF("Firstly, provide your name");
                output.flush();
            } catch (IOException e) {
                System.out.println("Error. Please try again");
            }
            return;
        }

        sendMsgToAllUsers(msg);
        fileSaver.save(msg, userName);
    }

    private void getHistory() throws IOException {
        List<String> lines = fileSaver.getHistory();

        String result = String.join(responseSeparator, lines);

        output.writeUTF(result);
        output.flush();
    }

    private void setUserName(String userName) {
        this.userName = userName;
        try {
            output.writeUTF("Added user");
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addMonitor(DataOutputStream outStream) {
        synchronized (addStreamMonitor) {
            outStreams.add(outStream);
        }
    }

    private void sendMsgToAllUsers(String msg) throws IOException {
        String formattedStr = Decorator.getFormattedStr(msg, userName);

        synchronized (sendMessageMonitor) {
            for (DataOutputStream out : outStreams) {
                out.writeUTF(formattedStr);
                out.flush();
            }
        }

    }

    private void deleteOutput(DataOutputStream output) {
        synchronized (deleteOutputMonitor) {
            outStreams.remove(output);
        }
    }


}
