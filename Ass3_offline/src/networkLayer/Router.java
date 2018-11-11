/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package networkLayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 *
 * @author samsung
 */
public class Router {
    private int routerId;
    private int numberOfInterfaces;
    private ArrayList<IPAddress> interfaceAddrs;//list of IP address of all interfaces of the router
    private ArrayList<RoutingTableEntry> routingTable;//used to implement DVR
    private ArrayList<Integer> neighborRouterIds;//Contains both "UP" and "DOWN" state routers
    private Boolean state;//true represents "UP" state and false is for "DOWN" state

    public Router() {
        interfaceAddrs = new ArrayList<>();
        routingTable = new ArrayList<>();
        neighborRouterIds = new ArrayList<>();
        
        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;
        
        numberOfInterfaces = 0;
    }
    
    public Router(int routerId, ArrayList<Integer> neighborRouters, ArrayList<IPAddress> interfaceAddrs)
    {
        this.routerId = routerId;
        this.interfaceAddrs = interfaceAddrs;
        this.neighborRouterIds = neighborRouters;
        routingTable = new ArrayList<>();
        
        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;
        
        numberOfInterfaces = this.interfaceAddrs.size();
    }

    @Override
    public String toString() {
        String temp = "";
        temp+="Router ID: "+routerId+"\n";
        temp+="Intefaces: \n";
        for(int i=0;i<numberOfInterfaces;i++)
        {
            temp+=interfaceAddrs.get(i).getString()+"\t";
        }
        temp+="\n";
        temp+="Neighbors: \n";
        for(int i=0;i<neighborRouterIds.size();i++)
        {
            temp+=neighborRouterIds.get(i)+"\t";
        }
        return temp;
    }
    
    
    
    /**
     * Initialize the distance(hop count) for each router.
     * for itself, distance=0; for any connected router with state=true, distance=1; otherwise distance=Constants.INFTY;
     */
    public void initiateRoutingTable()
    {
        int no_of_router = NetworkLayerServer.routers.size();

       // System.out.println("Initiating table of ID " + routerId + " size " + no_of_router);
       // if(routingTable.size()==0) firstInitiate=true;
        //else firstInitiate=false;
        for (int i = 0; i < no_of_router; i++) {
            Router router = NetworkLayerServer.routers.get(i);
            int id = router.routerId;
            double distance;
            int gateway_r_id = 0;
       //     if(!firstInitiate) gateway_r_id=routingTable.get(i).getGatewayRouterId();
            if (id == routerId) {
                distance = 0;
                gateway_r_id = id;
            } else if (checkIfNeighbour(id)) {
                if(NetworkLayerServer.getRouterFromId(id).getState()==true )distance = 1;
                else distance=Constants.INFTY;
                gateway_r_id = id;
            } else {
                distance = Constants.INFTY;
            }
                RoutingTableEntry entry = new RoutingTableEntry(id, distance, gateway_r_id);
            routingTable.add(entry);
//                if(firstInitiate)routingTable.add(entry);
//                else routingTable.set(i,entry);
            }



        
    }

    public boolean checkIfNeighbour(int id){
        for(int i=0;i<neighborRouterIds.size();i++){
            Router r=NetworkLayerServer.getRouterFromId(neighborRouterIds.get(i));
            if(id==neighborRouterIds.get(i)){
                return true;
            }
        }
        return  false;
    }
    
    /**
     * Delete all the routingTableEntry
     */
    public void clearRoutingTable()
    {

        routingTable.clear();
    }
    
    /**
     * Update the routing table for this router using the entries of Router neighbor
     * @param neighbor
     */
    public boolean updateRoutingTable_without_Split_Forced(Router neighbor)
    {
        boolean check=true;
        ArrayList<RoutingTableEntry> neighbour_table=neighbor.routingTable;
        double neighbour_router_distance=1;
        if(neighbor.getState()==false) neighbour_router_distance=Constants.INFTY;
        for(int i=0;i<routingTable.size();i++){
            RoutingTableEntry r1=routingTable.get(i);
            for(int j=0;j<neighbour_table.size();j++){
                RoutingTableEntry n1=neighbour_table.get(j);
                if(r1.getRouterId()==n1.getRouterId()){

                    double new_dis=n1.getDistance()+neighbour_router_distance;
                    if(new_dis<r1.getDistance()) {
                     //   System.out.println("Old distance= " +
                       //         r1.getDistance() + " New Distance " + new_dis);
                        if(new_dis > Constants.INFTY) new_dis=Constants.INFTY;
                        r1.setDistance(new_dis);
                        r1.setGatewayRouterId(neighbor.getRouterId());
                        check = false;
                       // System.out.println("distance of router " + r1.getRouterId() + " is changed");
                        break;

                    }
                }
            }
            routingTable.set(i,r1);
        }
        return check;
    }
    public boolean updateRoutingTable(Router neighbor)
    {
        boolean check=true;
        ArrayList<RoutingTableEntry> neighbour_table=neighbor.routingTable;
        double neighbour_router_distance=1;
        if(neighbor.getState()==false) neighbour_router_distance=Constants.INFTY;
       // System.out.println("changing router table of "+routerId);
        for(int i=0;i<routingTable.size();i++){
            if(routerId==routingTable.get(i).getRouterId()) continue;
            RoutingTableEntry r1=routingTable.get(i);
            for(int j=0;j<neighbour_table.size();j++){
                RoutingTableEntry n1=neighbour_table.get(j);
                if(r1.getRouterId()==n1.getRouterId()){
                    int destination=r1.getRouterId();
                    double new_dis=n1.getDistance()+neighbour_router_distance;
                  //  System.out.println("Gateway router for des: "+destination +
                    //        " of "+neighbor.getRouterId()+ " is "+ neighbor.getNextHop(destination));
                    if((getNextHop(destination)==neighbor.getRouterId())
                            ||(new_dis<r1.getDistance() && (routerId!= neighbor.getNextHop(destination)) )) {
                      //  System.out.println("Old distance= " +
                        //        r1.getDistance() + " New Distance " + new_dis);
                        if(new_dis > Constants.INFTY) new_dis=Constants.INFTY;
                        if(r1.getDistance() != new_dis){
                       //
                            r1.setDistance(new_dis);
                            r1.setGatewayRouterId(neighbor.getRouterId());
                            check = false;

         //                   System.out.println("distance of router " + r1.getRouterId() + " is changed");
                        }


                        break;

                    }
                }
            }
            routingTable.set(i,r1);
        }
        return check;
    }

    /**
     * If the state was up, down it; if state was down, up it
     */
    public void revertState()
    {
        state=!state;
      //  System.out.println("State is changed to "+state + " of router "+routerId);
        if(state==true) {
            this.initiateRoutingTable();
        }
        else this.clearRoutingTable();
    }
    
    public int getRouterId() {
        return routerId;
    }

    public void setRouterId(int routerId) {
        this.routerId = routerId;
    }

    public int getNumberOfInterfaces() {
        return numberOfInterfaces;
    }

    public void setNumberOfInterfaces(int numberOfInterfaces) {
        this.numberOfInterfaces = numberOfInterfaces;
    }

    public ArrayList<IPAddress> getInterfaceAddrs() {
        return interfaceAddrs;
    }

    public void setInterfaceAddrs(ArrayList<IPAddress> interfaceAddrs) {
        this.interfaceAddrs = interfaceAddrs;
        numberOfInterfaces = this.interfaceAddrs.size();
    }

    public ArrayList<RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public void addRoutingTableEntry(RoutingTableEntry entry) {
        this.routingTable.add(entry);
    }

    public ArrayList<Integer> getNeighborRouterIds() {
        return neighborRouterIds;
    }

    public void setNeighborRouterIds(ArrayList<Integer> neighborRouterIds) {
        this.neighborRouterIds = neighborRouterIds;
    }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }

    public void printRoutingTable(){
        System.out.println("table of Id :"+routerId);
        System.out.println("destination------distance------gateway");
        if(state==false) return;
        System.out.println("size of routing table is "+routingTable.size());
        for(int i=0;i<routingTable.size();i++){
            int gateway=routingTable.get(i).getGatewayRouterId();
            if(routingTable.get(i).getDistance()==Constants.INFTY) {
                System.out.println(routingTable.get(i).getRouterId()+"      "+routingTable.get(i).getDistance()
                        +"       ---");
            }
            else{
                System.out.println(routingTable.get(i).getRouterId()+"      "+routingTable.get(i).getDistance()
                        +"      "+gateway);
            }

        }
    }
    public int getNextHop(int to_id){
        for(int i=0;i<routingTable.size();i++){
            RoutingTableEntry re=routingTable.get(i);
            if(re.getRouterId()==to_id){
                return re.getGatewayRouterId();
            }
        }
        return 0;
    }
    
}
