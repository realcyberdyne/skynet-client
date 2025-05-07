package com.cyberdyne.skynet.client.Services.Functions;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyForward
{


    //Get constractor function
    public ProxyForward(int Port)
    {
        try
        {
            ServerSocket Server = new ServerSocket(Port);

            while (true)
            {
                Socket request=Server.accept();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try
                        {
                            GetHandleProxy(request);
                        }
                        catch (Exception e)
                        {

                        }
                    }
                }).start();
            }
        }
        catch (Exception e)
        {

        }
    }



    //Get handle request from socket
    public static void GetHandleProxy(Socket request)
    {
        try
        {
            //Get add forward socket
            Socket ForwardSocket = new Socket("192.168.1.3",8085);

            //Proxy streams
            BufferedWriter BW=new BufferedWriter(new OutputStreamWriter(request.getOutputStream()));
            BufferedReader BR=new BufferedReader(new InputStreamReader(request.getInputStream()));

            System.out.println("New request...");

            Thread CTS = ForwardRequestsAndReponses(ForwardSocket,request,"CTS",BR,BW);
            Thread STC = ForwardRequestsAndReponses(ForwardSocket,request,"STC",BR,BW);

            CTS.join();
            STC.join();
        }
        catch (Exception e)
        {

        }
    }



    //Get forward function start
    public static Thread ForwardRequestsAndReponses(Socket ForwardSocket,Socket request,String Direction,BufferedReader BR,BufferedWriter BW) throws Exception
    {
        Thread result = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream IS=ForwardSocket.getInputStream();
                    OutputStream OS=ForwardSocket.getOutputStream();

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while (true)
                    {

                        if(request.getInputStream().available() > 0)
                        {
                            bytesRead = request.getInputStream().read(buffer);
                            if (bytesRead == -1) break;
                            OS.write(buffer,0,bytesRead);
                            OS.flush();
                        }

                        if(ForwardSocket.getInputStream().available() > 0)
                        {
                            bytesRead = ForwardSocket.getInputStream().read(buffer);
                            if (bytesRead == -1) break;
                            request.getOutputStream().write(buffer,0,bytesRead);
                            request.getOutputStream().flush();
                        }

                        // Small delay to prevent tight spinning
//                Thread.sleep(10);

                    }


                }
                catch (Exception e)
                {

                }
            }
        });

        result.start();
        return result;
    }
    //Get forward function end


}