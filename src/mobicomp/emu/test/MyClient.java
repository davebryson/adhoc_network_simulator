package mobicomp.emu.test;

import mobicomp.emu.EmuSocket;
import mobicomp.emu.Emulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;


/**
 * @author Dave Bryson
 */
public class MyClient
{
    public static final String PING = "ping";
    public static final String PONG = "pong";
    private final int PORT = 5050;

    public MyClient()
    {
        try
        {
            InetAddress g = InetAddress.getByName("228.5.6.7");
            Server s = new Server(g);
            Pinger p = new Pinger(g);
            s.start();
            p.start();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
            Emulator.getRef().sendEmulatorMessage("Can't bind to Group Addy!",true);
        }
    }

    class Pinger extends Thread
    {
        MulticastSocket socket;
        InetAddress group;
        long interval = 5000;

        public Pinger(InetAddress g)
        {
            group = g;
            try
            {
                socket = new MulticastSocket();
                socket.joinGroup(group);
            }
            catch (IOException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        public void run()
        {
            while(true)
            {
                try
                {
                    byte[] buf = new byte[256];
                    buf = PING.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length,group,PORT);
                    socket.send(packet);
                    System.out.println("Sent Ping");
                    // sleep for a while
                    try
                    {
                        sleep((long)Math.random()+interval);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }
    class Server extends Thread
    {
        MulticastSocket socket;
        InetAddress group;

        public Server(InetAddress g)
        {
            group = g;
            try
            {
                socket = new MulticastSocket();
                socket.joinGroup(group);
            }
            catch (IOException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        public void run()
        {
            while(true)
            {
                try
                {
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String msg = "Got: " + new String(packet.getData());
                    System.out.println(msg);
                    byte[] buf2 = new byte[256];
                    buf2 = PONG.getBytes();
                    DatagramPacket Pongpacket = new DatagramPacket(buf2, buf2.length,group,PORT);
                    socket.send(Pongpacket);
                }
                catch (IOException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

        }
    }

    public static void main(String[] args)
    {
        new MyClient();
    }
}
