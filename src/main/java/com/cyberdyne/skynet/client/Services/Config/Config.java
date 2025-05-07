package com.cyberdyne.skynet.client.Services.Config;

import java.io.FileReader;
import java.util.Properties;

public class Config
{

    //Global variables
    public static int VPNPort;
    public static int PresentPort;


    //Get constractor
    public Config()
    {

        //Get read file
        try
        {
            FileReader reader = new FileReader("Settings.properties");
            Properties properties = new Properties();
            properties.load(reader);

            //Proxy internal port repository address
            VPNPort=Integer.parseInt(properties.getProperty("VPNPort").toString());
            PresentPort=Integer.parseInt(properties.getProperty("PresentPort").toString());

        }
        catch (Exception e)
        {
            //Print error
            System.out.println("Config error : "+e.getMessage());
        }


    }


}
