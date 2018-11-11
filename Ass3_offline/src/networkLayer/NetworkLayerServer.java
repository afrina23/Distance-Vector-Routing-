/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package networkLayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CSE_BUET
 */
public class NetworkLayerServer {
    static int clientCount = 1;
    static ArrayList<Router> routers = new ArrayList<>();
    static RouterStateChanger stateChanger = null;
    /**
     * Each map entry represents number of client end devices connected to the interface
     */
    static Map<IPAddress,Integer> clientInterfaces = new HashMap<>();
    static Map<EndDevice,SocketStream> client_socket_map= new HashMap<>();
    static ArrayList<EndDevice> clientList= new ArrayList<>();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /**
         * Task: Maintain an active client list
         */
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(1234);
        } catch (IOException ex) {
            Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Server Ready: "+serverSocket.getInetAddress().getHostAddress());
        
        System.out.println("Creating router topology");
        
        readTopology();
        printRouters();

        
        /**
         * Initialize routing tables for all routers
         */
        initRoutingTables();
        printRoutingTables();
        /**
         * Update routing table using distance vector routing until convergence
         */
        DVR(1);
       // simpleDVR(1);
        
        /**
         * Starts a new thread which turns on/off routers randomly depending on parameter Constants.LAMBDA
         */

        stateChanger = new RouterStateChanger();


        while(true){
           try {
                Socket clientSock = serverSocket.accept();
                System.out.println("Client attempted to connect");
                EndDevice endDevice=getClientDeviceSetup();
               // client_socket_map.put(endDevice,clientSock);
                clientList.add(endDevice);
                new ServerThread(clientSock,endDevice);
           } catch (IOException ex) {
                Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
           }
        }


    }
    
    public static void initRoutingTables()
    {


        for(int i=0;i<routers.size();i++)
        {
            routers.get(i).initiateRoutingTable();
        }

    }


    /**
     * Task: Implement Distance Vector Routing with Split Horizon and Forced Update
     */
    public static void DVR(int startingRouterId)
    {
        /**
         * pseudocode
         */
        /*
        while(convergence)
        {
            //convergence means no change in any routingTable before and after executing the following for loop
            for each router r <starting from the router with routerId = startingRouterId, in any order>
            {
                1. T <- getRoutingTable of the router r
                2. N <- find routers which are the active neighbors of the current router r
                3. Update routingTable of each router t in N using the 
                   routing table of r [Hint: Use t.updateRoutingTable(r)]
            }
        }
        */
        ArrayList<Router> router_map = new ArrayList<>();
        router_map=BFS(getRouterFromId(startingRouterId));

        boolean check_convergence=false;
        int no_of_iteration=0;
        while(true){
            if(check_convergence){
                break;
            }
            check_convergence=true;
            no_of_iteration++;
           // System.out.println("Iteration no "+no_of_iteration);

            for(int i=0;i<router_map.size();i++){
                Router present=router_map.get(i);



             //   System.out.println("neighbours of id "+present.getRouterId()+ " are "+neighbours.size());
                for(int j=0;j<routers.size();j++){
                    if(present.getNeighborRouterIds().contains(routers.get(j).getRouterId())){
                        Router neibour_router=routers.get(j);
               //         System.out.println("State if router "+neibour_router.getRouterId()+
                   //     " is "+neibour_router.getState());
                        if(neibour_router.getState()==false) continue;
                 //       System.out.println("present Id :"+present.getRouterId()+ " neighbour Id "
                   //             +neibour_router.getRouterId());
                        boolean check=neibour_router.updateRoutingTable(present);
                        if(check==false){
                            check_convergence=check;
                        }


                     //   System.out.println("check convergence is "+check_convergence);

                    }

                }

            }
//            System.out.println("--------iteration --- "+no_of_iteration);
        }
        printRoutingTables();
    }
    public static ArrayList<Router> getNeighbours(Router router){
        ArrayList<Router> neighbours= new ArrayList<>();
        ArrayList<Integer> neighbour_of_router= router.getNeighborRouterIds();
  //      System.out.println("neighbours of "+router.getRouterId()+" are ");
        for(int i=0;i<routers.size();i++){
            if(neighbour_of_router.contains(routers.get(i).getRouterId())){
    //            System.out.print(routers.get(i).getRouterId()+"   ");
                neighbours.add(routers.get(i));
            }
        }
        return neighbours;
    }
    
    /**
     * Task: Implement Distance Vector Routing without Split Horizon and Forced Update
     */
    public static void simpleDVR(int startingRouterId)
    {
        ArrayList<Router> router_map;
        router_map=BFS(getRouterFromId(startingRouterId));

        boolean check_convergence=false;
        int no_of_iteration=0;
        while(true){
            if(check_convergence){
                break;
            }
            check_convergence=true;
            no_of_iteration++;
            System.out.println("Iteration no "+no_of_iteration);

            for(int i=0;i<router_map.size();i++){
                Router present=router_map.get(i);

                ArrayList<Router> neighbours=getNeighbours(present);

            //    System.out.println("neighbours of id "+present.getRouterId()+ " are "+neighbours.size());
                for(int j=0;j<neighbours.size();j++){

                    Router neibour_router=neighbours.get(j);
                    if(neibour_router.getState()==false) continue;
                 //   System.out.println("present Id :"+present.getRouterId()+ " neighbour Id "
                    //        +neibour_router.getRouterId());
                    if(check_convergence){
                        check_convergence=neibour_router.
                                updateRoutingTable_without_Split_Forced(present);
                    }


                //    System.out.println("check convergence is "+check_convergence);

                }

            }

        }
        
    }
    
    
    public static EndDevice getClientDeviceSetup()
    {
        Random random = new Random();
        int r =Math.abs(random.nextInt(clientInterfaces.size()));
        
        System.out.println("Size: "+clientInterfaces.size()+"\n"+r);
        
        IPAddress ip=null;
        IPAddress gateway=null;
        
        int i=0;
        for (Map.Entry<IPAddress, Integer> entry : clientInterfaces.entrySet()) {
            IPAddress key = entry.getKey();
            Integer value = entry.getValue();
            if(i==r)
            {
                gateway = key;
                ip = new IPAddress(gateway.getBytes()[0]+"."+gateway.getBytes()[1]+"."+gateway.getBytes()[2]+"."+(value+2));
                value++;
                clientInterfaces.put(key, value);
                break;
            }
            i++;
        }
        
        EndDevice device = new EndDevice(ip, gateway);
        System.out.println("Device : "+ip+"::::"+gateway);
        return device;
    }
    
    public static void printRouters()
    {
        for(int i=0;i<routers.size();i++)
        {
            System.out.println("------------------\n"+routers.get(i));
        }
    }
    public static void printRoutingTables(){
        for(int i=0;i<routers.size();i++)
        {
            System.out.println("------------------\n");
            routers.get(i).printRoutingTable();
        }
    }
    
    public static void readTopology()
    {
        Scanner inputFile = null;
        try {
            inputFile = new Scanner(new File("topology_mine.txt"));
            //skip first 27 lines
            int skipLines = 27;
            for(int i=0;i<skipLines;i++)
            {
                inputFile.nextLine();
            }
            
            //start reading contents
            while(inputFile.hasNext())
            {
                inputFile.nextLine();
                int routerId;
                ArrayList<Integer> neighborRouters = new ArrayList<>();
                ArrayList<IPAddress> interfaceAddrs = new ArrayList<>();
                
                routerId = inputFile.nextInt();
                
                int count = inputFile.nextInt();
                for(int i=0;i<count;i++)
                {
                    neighborRouters.add(inputFile.nextInt());
                }
                count = inputFile.nextInt();
                inputFile.nextLine();
                
                for(int i=0;i<count;i++)
                {
                    String s = inputFile.nextLine();
                    //System.out.println(s);
                    IPAddress ip = new IPAddress(s);
                    interfaceAddrs.add(ip);
                    
                    /**
                     * First interface is always client interface
                     */
                    if(i==0)
                    {
                        //client interface is not connected to any end device yet
                        clientInterfaces.put(ip, 0);
                    }
                }
                Router router = new Router(routerId, neighborRouters, interfaceAddrs);
                routers.add(router);
            }
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static Router getRouterFromId(int id){
        Router r=null;
        for(int i=0;i<routers.size();i++){
            if(routers.get(i).getRouterId()==id){
                r=routers.get(i);
                break;
            }
        }
        return r;
    }
    public static ArrayList<Router> BFS(Router s)
    {
        ArrayList<Router> bfs_routers=new ArrayList<>();
        LinkedList<Router> queue = new LinkedList<Router>();

        // Mark the current node as visited and enqueue it

        queue.add(s);

        while (queue.size() != 0)
        {
            // Dequeue a vertex from queue and print it
            s = queue.poll();
            System.out.print(s.getRouterId()+" ");
            bfs_routers.add(s);
            ArrayList<Integer> neighbours=s.getNeighborRouterIds();
            for(int ii=0;ii<neighbours.size();ii++){
                Router n=getRouterFromId(neighbours.get(ii));
                if(!bfs_routers.contains(n)){
                    bfs_routers.add(n);
                    queue.add(n);
                }
            }

        }
        if(bfs_routers.size() != routers.size()){
            for(int i=0;i<routers.size();i++){
                if(!bfs_routers.contains(routers.get(i))){
                    bfs_routers.add(routers.get(i));
                }
            }
        }
        return bfs_routers;
    }

}
