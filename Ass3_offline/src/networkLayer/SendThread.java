package networkLayer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

/**
 * Created by Afrina on 14-Nov-17.
 */
public class SendThread implements Runnable{
    Socket socket;
    ObjectInputStream input = null;
    ObjectOutputStream output;
    ArrayList<EndDevice> clientList;
    EndDevice endDevice;
    int hopCount;
    public int dropped_packet=0;
    Thread t;
    Random random;
    String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    String lower = upper.toLowerCase(Locale.ROOT);

    String digits = "0123456789";


    SendThread(Socket socket,ObjectInputStream ois,ObjectOutputStream oos,EndDevice endDevice){
        this.socket=socket;
        this.endDevice=endDevice;
        this.clientList=new ArrayList<>();
        random= new Random();
        t= new Thread(this);

        try {
            input = ois;
            output = oos;
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        t.start();
    }


    @Override
    public void run() {

        hopCount=0;

        for(int i=0;i<100;i++){
            String message=generateString();

            System.out.println("the message is "+message+ " and msg no "+(i));
            Packet packet=createPacket(message);
            if(i==20){
                packet.setSpecialMessage("SHOW_ROUTE");

                try {
                    output.writeObject(packet);
                    System.out.println("waiting for reply message");
                    Packet reply= (Packet) input.readObject();
                    System.out.println("Got reply message");
                    showRoute(reply);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
            else{
                try {
                    output.writeObject(packet);
                    System.out.println("packet is written");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            try {
                Packet ack= (Packet) input.readObject();
                if(ack.getMessage().equals("failure")){
                    System.out.println("Packet is dropped");
                    dropped_packet++;
                }
                else{
                    System.out.println("Packet sent successfully and hop count is "+ack.getHopCount());
                    hopCount+=ack.getHopCount();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }


        }
//        try {
//
//       //     output.writeObject("complete");
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        System.out.println("Number of dropped packet is "+dropped_packet);
        double avg_hop=hopCount/100.00;
        double drop_rate=dropped_packet/100.00;
        System.out.println("Average Hop count is "+avg_hop+"\n Drop Rate is "+drop_rate);
        /*
        for(int i=0;i<100;i++)
        4. {
        5.      Generate a random message
        6.      Assign a random receiver from active client list
        7.      if(i==20)
        8.      {
        9.            Send the message and recipient IP address to server and a special request "SHOW_ROUTE"
        10.           Display routing path, hop count and routing table of each router [You need to receive
                            all the required info from the server in response to "SHOW_ROUTE" request]
        11.     }
        12.     else
        13.     {
        14.           Simply send the message and recipient IP address to server.
        15.     }
        16.     If server can successfully send the message, client will get an acknowledgement along with hop count
                    Otherwise, client will get a failure message [dropped packet]
        17. }
         */
    }
    public void showRoute(Packet reply){
        String routerRoutes=reply.getRouterRoute();
        HashMap<Integer,ArrayList<RoutingTableEntry>> tables=reply.routingTables;
        System.out.println("The route is given here");
        System.out.println(routerRoutes);
        System.out.println("Routing tables are ");
        for (Map.Entry<Integer, ArrayList<RoutingTableEntry>> e : tables.entrySet()) {
            //to get key
            System.out.println("Ruting Table of ID :"+e.getKey()+" is ");
            //and to get value
            ArrayList<RoutingTableEntry> table=e.getValue();
            System.out.println("destination------distance------gateway");
            for(int i=0;i<table.size();i++){
                System.out.println(table.get(i).getRouterId()+"      "+table.get(i).getDistance()
                        +"      "+table.get(i).getGatewayRouterId());
            }
        }
    }
    public Packet createPacket(String message){
        Packet newPacket= new Packet();
        newPacket.setMessage(message);
      //  newPacket.setDestinationIP(clientList.get(client_no).getIp());
        newPacket.setSourceIP(endDevice.getIp());
        return newPacket;
    }
    public String generateString()
    {
        int length=10;
        String alphanum = upper + lower + digits;
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = alphanum.charAt(random.nextInt(alphanum.length()));
        }
        return new String(text);
    }
}
