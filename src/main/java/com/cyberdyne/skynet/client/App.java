package com.cyberdyne.skynet.client;

import com.cyberdyne.skynet.client.Services.Config.Config;
import com.cyberdyne.skynet.client.Services.Functions.Proxy;

public class App
{
    public static void main(String[] args)
    {
        //Get config from file
        new Config();

        new Proxy(8086);

    }
}