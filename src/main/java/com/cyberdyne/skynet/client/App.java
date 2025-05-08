package com.cyberdyne.skynet.client;

import com.cyberdyne.skynet.client.Services.Config.Config;
import com.cyberdyne.skynet.client.Services.Functions.Proxy;
import com.cyberdyne.skynet.client.Services.Functions.ProxyVPNForward;
import com.cyberdyne.skynet.client.Services.Functions.VPNForward;

public class App
{
    public static void main(String[] args)
    {
        //Get config from file
        new Config();

        new VPNForward("192.168.1.3",8086,"reza");

    }
}