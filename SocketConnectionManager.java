package com.example.trivia;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketConnectionManager {

    private static final String TAG = "SocketConnectionManager";

    private static SocketConnectionManager instance;
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private Thread connectionThread;

    private SocketConnectionManager() {
        // Private constructor to prevent external instantiation
    }

    public static synchronized SocketConnectionManager getInstance() {
        if (instance == null) {
            instance = new SocketConnectionManager();
        }
        return instance;
    }

    public boolean connect(String host, int port) {
        if (connectionThread != null && connectionThread.isAlive()) {
            Log.w(TAG, "Already connected to server");
            return false;
        }

        connectionThread = new Thread(() -> {
            try {
                socket = new Socket(host, port);
                output = new PrintWriter(socket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.d(TAG, "Connected to server: " + host + ":" + port);
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to server: " + e.getMessage());
            }
        });
        connectionThread.start();
        return true;
    }

    public void disconnect() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
                Log.d(TAG, "Disconnected from server");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error disconnecting from server: " + e.getMessage());
        } finally {
            socket = null;
            output = null;
            input = null;
            connectionThread = null; // איפוס ה-thread
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public Socket getSocket() {
        return socket;
    }

    public String sendAndReceive(String message) {
        try {
            if (socket != null && socket.isConnected()) {
                output.println(message);

                char[] buffer = new char[1024];
                int charsRead = input.read(buffer);
                String response = new String(buffer, 0, charsRead);

                // פענוח התגובה באמצעות UTF-8
                response = new String(response.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

                return response;
            } else {
                return "Not connected to server";
            }
        } catch (IOException e) {
            Log.e(TAG, "Error sending/receiving message: " + e.getMessage());
            return "Error communicating with server";
        }
    }
}