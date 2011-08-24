package mobicomp.emu.test;

import org.json.JSONException;
import org.json.JSONObject;
import java.net.*;

/**
 * @author Dave Bryson
 */
public class SimpleClient
{
    InetAddress me,seed;
    private static final int PORT = 8001;
    DatagramSocket socket;
    private int state;

    private static final int SEEDING = 1;
    private static final int CONNECTED = 2;

    public SimpleClient()
            throws Exception
    {
        //state = SEEDING;
        socket = new DatagramSocket(PORT);
        this.seed = InetAddress.getByName("228.5.6.7");

        new Processor().start();
        new Thread(new Seed()).start();


    }

    public class Telex
    {
        public String to, from;

        public Telex()
        {
        }

        public Telex(String data)
                throws JSONException
        {
            JSONObject obj = new JSONObject(data);
            this.to = obj.getString("to");
            this.from = obj.getString("from");
        }

        public String toJSON()
                throws JSONException
        {
            JSONObject obj = new JSONObject();
            obj.put("to", to);
            obj.put("from", from);
            return obj.toString();
        }

        public String toString()
        {
            return "TO: " + to + " FROM: " + from;
        }

    }

    class Seed implements Runnable
    {
        public void run()
        {
            try
            {
                MulticastSocket outty = new MulticastSocket();
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length,seed,PORT);
                outty.send(packet);

            }
            catch (Exception e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }


    class Processor extends Thread
    {
        static final long DELAY = 6000;

        public void run()
        {
            while (true)
            {
                try
                {
                    // REQUEST
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    Telex t = new Telex(new String(packet.getData()));
                    System.out.println("Got Telex: " + t.toString());


                    try
                    {
                        sleep(DELAY);
                    }
                    catch (InterruptedException e)
                    {
                    }

                    // RESPONSE
                    t.from = me.toString(); // ME
                    t.to = t.from;
                    byte[] buf2 = t.toJSON().getBytes();
                    DatagramPacket packet2 = new DatagramPacket(buf2, buf2.length);
                    socket.send(packet2);
                }
                catch (Exception e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

        }
    }

    public static void main(String[] args)
    {
        try
        {

            new SimpleClient();
        }
        catch (Exception e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
