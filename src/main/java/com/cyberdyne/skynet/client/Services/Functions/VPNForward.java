package com.cyberdyne.skynet.client.Services.Functions;

import com.cyberdyne.skynet.client.Services.Config.Config;
import com.cyberdyne.skynet.client.Services.Encription.EncriptionBytesCLS;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VPNForward
{
    private final ExecutorService threadPool;
    private final String encryptionKey;

    // Constructor with encryption key parameter
    public VPNForward(String VPNHostAddress, int Port, String encryptKey)
    {
        // Store encryption key
        this.encryptionKey = encryptKey;

        // Create a thread pool instead of creating unlimited threads
        threadPool = Executors.newCachedThreadPool();

        try
        {
            ServerSocket Server = new ServerSocket(Port);
            System.out.println("VPN client started on port " + Port);

            while (true)
            {
                Socket request = Server.accept();
                System.out.println("New connection from " + request.getInetAddress());

                threadPool.submit(() -> {
                    try
                    {
                        GetHandleProxy(VPNHostAddress, request);
                    }
                    catch (Exception e)
                    {
                        System.err.println("Error handling proxy request: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }
        catch (Exception e)
        {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
            threadPool.shutdown();
        }
    }

    // Handle proxy request with encryption
    public void GetHandleProxy(String VPNHostAddress, Socket localClient)
    {
        Socket serverSocket = null;

        try
        {
            // Get the HTTP request headers
            InputStream localInput = localClient.getInputStream();
            OutputStream localOutput = localClient.getOutputStream();

            // Wait for data to be available
            while (localInput.available() == 0) {
                Thread.sleep(5);
            }

            // Buffer for reading the CONNECT request
            byte[] buffer = new byte[8192];
            int bytesRead = localInput.read(buffer);

            // Convert to string for parsing
            String request = new String(buffer, 0, bytesRead);

            // Parse the first line of the HTTP request
            String[] lines = request.split("\r\n");
            String firstLine = lines[0];

            if (!firstLine.startsWith("CONNECT")) {
                System.err.println("Not a CONNECT request: " + firstLine);
                localClient.close();
                return;
            }

            // Connect to the VPN server
            serverSocket = new Socket(VPNHostAddress, Config.VPNPort);
            System.out.println("Connected to VPN at " + VPNHostAddress + ":" + Config.VPNPort);

            // Get the streams
            InputStream serverInput = serverSocket.getInputStream();
            OutputStream serverOutput = serverSocket.getOutputStream();

            // Encrypt and send the CONNECT request to the VPN server
            byte[] encryptedRequest = EncriptionBytesCLS.encrypt(request.getBytes(), encryptionKey);
            serverOutput.write(encryptedRequest);
            serverOutput.flush();
            System.out.println("Sent encrypted CONNECT request to VPN server");

            // Wait for server response (200 Connection Established)
            while (serverInput.available() == 0) {
                Thread.sleep(5);
            }

            // Read server response
            byte[] responseBuffer = new byte[1024];
            int responseBytes = serverInput.read(responseBuffer);
            String response = new String(responseBuffer, 0, responseBytes);

            // Check if connection was established
            if (!response.contains("200 Connection Established")) {
                System.err.println("VPN server connection failed: " + response);
                localClient.close();
                serverSocket.close();
                return;
            }

            // Send 200 Connection Established to local client
            localOutput.write(responseBuffer, 0, responseBytes);
            localOutput.flush();
            System.out.println("Connection established with client");

            // Start bidirectional data transfer
            Thread clientToServer = encryptAndForward(localClient, serverSocket);
            Thread serverToClient = forwardToClient(serverSocket, localClient);

            // Wait for both threads to complete
            clientToServer.join();
            serverToClient.join();

        } catch (Exception e) {
            System.err.println("Proxy handling error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                if (!localClient.isClosed()) {
                    localClient.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing sockets: " + e.getMessage());
            }
        }
    }

    // Encrypt client data and forward to server
    private Thread encryptAndForward(Socket client, Socket server) {
        Thread thread = new Thread(() -> {
            try {
                InputStream clientInput = client.getInputStream();
                OutputStream serverOutput = server.getOutputStream();
                byte[] buffer = new byte[8192];

                while (!client.isClosed() && !server.isClosed()) {
                    if (clientInput.available() > 0) {
                        int bytesRead = clientInput.read(buffer);
                        if (bytesRead <= 0) break;

                        // Create a properly sized array for the actual data
                        byte[] dataToEncrypt = new byte[bytesRead];
                        System.arraycopy(buffer, 0, dataToEncrypt, 0, bytesRead);

                        try {
                            // Encrypt the data
                            byte[] encryptedData = EncriptionBytesCLS.encrypt(dataToEncrypt, encryptionKey);

                            // Send encrypted data to the server
                            serverOutput.write(encryptedData);
                            serverOutput.flush();
                            System.out.println("Client → Server: Encrypted and forwarded " + bytesRead +
                                    " bytes → " + encryptedData.length + " bytes");
                        } catch (Exception e) {
                            System.err.println("Encryption error: " + e.getMessage());
                            break;
                        }
                    } else {
                        // Sleep to prevent CPU spinning
                        Thread.sleep(5);
                    }
                }
            } catch (Exception e) {
                System.err.println("Client → Server forwarding error: " + e.getMessage());
            }
        });

        thread.start();
        return thread;
    }

    // Forward data from server to client (no encryption/decryption)
    private Thread forwardToClient(Socket server, Socket client) {
        Thread thread = new Thread(() -> {
            try {
                InputStream serverInput = server.getInputStream();
                OutputStream clientOutput = client.getOutputStream();
                byte[] buffer = new byte[8192];

                while (!server.isClosed() && !client.isClosed()) {
                    if (serverInput.available() > 0) {
                        int bytesRead = serverInput.read(buffer);
                        if (bytesRead <= 0) break;

                        // Forward data directly to the client
                        clientOutput.write(buffer, 0, bytesRead);
                        clientOutput.flush();
                        System.out.println("Server → Client: Forwarded " + bytesRead + " bytes");
                    } else {
                        // Sleep to prevent CPU spinning
                        Thread.sleep(5);
                    }
                }
            } catch (Exception e) {
                System.err.println("Server → Client forwarding error: " + e.getMessage());
            }
        });

        thread.start();
        return thread;
    }
}