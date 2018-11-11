/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package networkLayer;



import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static networkLayer.NetworkLayerServer.client_socket_map;

/**
 *
 * @author samsung
 */
public class ServerThread implements Runnable {
    private Thread t;
    private Socket socket;
    EndDevice endDevice;
    Random random;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    
    public ServerThread(Socket socket,EndDevice endDevice){
        
        this.socket = socket;
        random= new Random();
        this.endDevice=endDevice;
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Server Ready for client "+NetworkLayerServer.clientCount);
        SocketStream ss=new SocketStream(socket,output,input);
        client_socket_map.put(endDevice,ss);
        NetworkLayerServer.clientCount++;
        
        t=new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        /**
         * Synchronize actions with client.
         */
        /*
        Tasks:
        1. Upon receiving a packet and recipient, call deliverPacket(packet)
        2. If the packet contains "SHOW_ROUTE" request, then fetch the required information
                and send back to client
        3. Either send acknowledgement with number of hops or send failure message back to client
        */
        try {
            output.writeObject(endDevice);
            int packet_no=0;
            while(true){
                Object msg=input.readObject();
                if(msg instanceof String ){
                    String m= (String) msg;
                    if(m.equals("complete")){
                        break;
                    }
                    else{
                        NetworkLayerServer.clientCount--;
                        NetworkLayerServer.clientList.remove(endDevice);
                        client_socket_map.remove(endDevice);
                        break;
                    }

                }

                Packet packet=(Packet) msg;
                System.out.println("Received packet no "+packet_no);
                packet_no++;
                IPAddress destionationIp;
                do{
                    int client_no=random.nextInt(NetworkLayerServer.clientList.size());

                    destionationIp=NetworkLayerServer.clientList.get(client_no).getIp();
                    //System.out.println("receiver is --"+destionationIp);

                }while (packet.getSourceIP().getString().equals(destionationIp.getString()));
               // int client_no=random.nextInt(NetworkLayerServer.clientList.size());
                System.out.println("Sender is --"+packet.getSourceIP());
                System.out.println("receiver is --"+destionationIp);
             //   System.out.println("receiver is --"+NetworkLayerServer.clientList.get(client_no).getIp());
                packet.setDestinationIP(destionationIp);

               // System.out.println("special message is "+packet.getSpecialMessage()+" message is "+
                //packet.getMessage());
                boolean result=deliverPacket(packet);

                if(packet.getSpecialMessage().equals("SHOW_ROUTE")){
                    Packet reply=new Packet();
                    reply.setHopCount(packet.getHopCount());
                    reply.setRouterRoute(packet.getRouterRoute());
                    System.out.println("Routing route is "+packet.getRouterRoute());
                    HashMap<Integer,ArrayList<RoutingTableEntry>> tables=new HashMap<>();
                    for(int e=0;e<NetworkLayerServer.routers.size();e++){
                        Router r=NetworkLayerServer.routers.get(e);
                        tables.put(r.getRouterId(),r.getRoutingTable());
                    }
                    reply.routingTables=tables;
                    System.out.println("Reply is ready to send");
                    output.writeObject(reply);
                    System.out.println("reply is written");
                }
                if(result==false){
                    Packet ack=new Packet();
                    ack.setMessage("failure");
                    output.writeObject(ack);
                }

                else{
                    Packet ack=new Packet();
                    ack.setMessage("success");
                    ack.setHopCount(packet.getHopCount());
                    output.writeObject(ack);
                   // sendToReceiver(packet);
                }

                System.out.println("packet  deliver is finised and route is "+packet.getRouterRoute());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Send is finished");

    }
    public void sendToReceiver(Packet packet){
        IPAddress receiverIP=packet.getDestinationIP();
        EndDevice receiver=null;
        for(int i=0;i<NetworkLayerServer.clientList.size();i++){
           // System.out.println("Receiver ip "+receiverIP+" list Ip "
             //       +NetworkLayerServer.clientList.get(i).getIp());
            if(receiverIP.equals(NetworkLayerServer.clientList.get(i).getIp())){
                receiver=NetworkLayerServer.clientList.get(i);
            }
        }
        if(receiver==null){
            System.out.println("receiver is not online");
            return;
        }
        else{
            SocketStream receiverSocket= client_socket_map.get(receiver);
            try {
                System.out.println("sending packet to receiver");
                receiverSocket.output.writeObject(packet);
                System.out.println("sent packet to receiver");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    

    public Boolean deliverPacket(Packet p)
    {
        /*
        1. Find the router s which has an interface
                such that the interface and source end device have same network address.
        2. Find the router d which has an interface
                such that the interface and destination end device have same network address.
        3. Implement forwarding, i.e., s forwards to its gateway router x considering d as the destination.
                similarly, x forwards to the next gateway router y considering d as the destination, 
                and eventually the packet reaches to destination router d.
                
            3(a) If, while forwarding, any gateway x, found from routingTable of router r is in down state[x.state==FALSE]
                    (i) Drop packet
                    (ii) Update the entry with distance Constants.INFTY
                    (iii) Block NetworkLayerServer.stateChanger.t
                    (iv) Apply DVR starting from router r.
                    (v) Resume NetworkLayerServer.stateChanger.t
                            
            3(b) If, while forwarding, a router x receives the packet from router y, 
                    but routingTableEntry shows Constants.INFTY distance from x to y,
                    (i) Update the entry with distance 1
                    (ii) Block NetworkLayerServer.stateChanger.t
                    (iii) Apply DVR starting from router x.
                    (iv) Resume NetworkLayerServer.stateChanger.t
                            
        4. If 3(a) occurs at any stage, packet will be dropped, 
            otherwise successfully sent to the destination router
        */
        Router sourceRouter=getRouterfromInterface(p.getSourceIP());
       // Router sourceRouter=NetworkLayerServer.getRouterFromId(1);
        System.out.println("Source router is "+sourceRouter.getRouterId());
        Router destinationRouter=getRouterfromInterface(p.getDestinationIP());
        //Router destinationRouter=NetworkLayerServer.getRouterFromId(9);
        System.out.println("Destination router is "+destinationRouter.getRouterId());

        for(int e=0;e<NetworkLayerServer.routers.size();e++){
            System.out.println("State of router "+NetworkLayerServer.routers.get(e).getRouterId()
                    + " is "+NetworkLayerServer.routers.get(e).getState());
        }

        Router middleRouter=sourceRouter;
        if(middleRouter.getState()==false){
            System.out.println("packet is dropped because source router is down");
            p.setRouterRoute(p.getRouterRoute()+"(dropped)");
            //blockAndDVR(middleRouter);
            //  NetworkLayerServer.printRoutingTables();
            return false;
        }
        p.setHopCount(p.getHopCount());
        p.setRouterRoute(String.valueOf(sourceRouter.getRouterId()));

        while(middleRouter!= destinationRouter){
            int gateWay=middleRouter.getNextHop(destinationRouter.getRouterId());
            //System.out.println("GateWay is "+gateWay);
            if(gateWay==0){
                System.out.println("Gateway router is Down or no route between them");
                p.setRouterRoute(p.getRouterRoute()+"(dropped)");
                return false;
            }
            Router gateWayRouter=NetworkLayerServer.getRouterFromId(gateWay);
            if(gateWayRouter.getState()==false){
               // System.out.println("gateway state is false");
                for(int ii=0;ii<middleRouter.getRoutingTable().size();ii++){
                    RoutingTableEntry r1=middleRouter.getRoutingTable().get(ii);
                    if(r1.getRouterId()==gateWay){
                        if(r1.getDistance()==Constants.INFTY){
                            System.out.println(gateWay+ " router is down ");
                            p.setRouterRoute(p.getRouterRoute()+"(dropped)");
                            return false;
                        }
                        System.out.println("state is false but dis is 1");
                        r1.setDistance(Constants.INFTY);
                        blockAndDVR(middleRouter);
                        p.setRouterRoute(p.getRouterRoute()+"(dropped)");
                        break;
                    }
                }


             //   NetworkLayerServer.printRoutingTables();
                return false;
            }
            else{
                for(int ii=0;ii<middleRouter.getRoutingTable().size();ii++){
                    RoutingTableEntry r1=middleRouter.getRoutingTable().get(ii);
                    if(r1.getRouterId()==gateWay){
                        for(int j=0;j<gateWayRouter.getRoutingTable().size();j++){
                            RoutingTableEntry g1=gateWayRouter.getRoutingTable().get(j);
                            if(g1.getRouterId()==middleRouter.getRouterId()){
                                if(g1.getDistance()==Constants.INFTY){
                                    System.out.println("Router is up but distance" +
                                            " from gateway is infty");
                                    g1.setDistance(1);
                                    blockAndDVR(gateWayRouter);
                                }
                            }
                        }

                        break;
                    }
                }
                p.setHopCount(p.getHopCount());

                System.out.println("packet sent from router "+middleRouter.getRouterId()
                        +" to router "+gateWayRouter.getRouterId());
                middleRouter=gateWayRouter;
                p.setRouterRoute(p.getRouterRoute()+"->"+middleRouter.getRouterId());

            }

        }
        p.setHopCount(p.getHopCount()+1);

        return true;
    }
    public static void blockAndDVR(Router start){
        NetworkLayerServer.stateChanger.pause();
        System.out.println("Starting DVR from id"+start.getRouterId());
        NetworkLayerServer.DVR(start.getRouterId());
       // NetworkLayerServer.simpleDVR(start.getRouterId());
        NetworkLayerServer.printRoutingTables();
        System.out.println("finished DVR from id "+start.getRouterId());
        NetworkLayerServer.stateChanger.play();
    }
    public Router getRouterfromInterface(IPAddress ipAddress){

       for(int i=0;i<NetworkLayerServer.routers.size();i++){
           if(searchForNetworkAddress(NetworkLayerServer.routers.get(i),ipAddress)){
               return NetworkLayerServer.routers.get(i);
           }
       }
        System.out.println("No router found");
        return null;
    }
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }
    public boolean searchForNetworkAddress(Router r,IPAddress ipAddress){
        for(int i=0;i<r.getNumberOfInterfaces();i++){
            IPAddress interIp=r.getInterfaceAddrs().get(i);
            Short[] ip1=interIp.getBytes();
            Short[] ip2=ipAddress.getBytes();
            if((ip1[0].equals(ip2[0]))&&(ip1[1].equals(ip2[1]))&&(ip1[2].equals(ip2[2]))){
                return true;
            }
        }
        return false;
    }

}
