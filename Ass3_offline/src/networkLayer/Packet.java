/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package networkLayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author samsung
 */
public class Packet implements Serializable{
    private String message;
    private String specialMessage;//ex: "SHOW_ROUTE" request


    private String routerRoute="";
    private IPAddress destinationIP;
    private IPAddress sourceIP;
    private int hopCount;
    public HashMap<Integer,ArrayList<RoutingTableEntry> > routingTables;

    public Packet(){
        routingTables=new HashMap<>();
        specialMessage="";
        message="";
        hopCount=0;

    }

    public Packet(String message, String specialMessage, IPAddress sourceIP, IPAddress destinationIP) {
        this.message = message;
        hopCount=0;
        this.specialMessage = specialMessage;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        routingTables=new HashMap<>();
    }

    public IPAddress getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(IPAddress sourceIP) {
        this.sourceIP = sourceIP;
    }
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSpecialMessage() {
        return specialMessage;
    }

    public void setSpecialMessage(String specialMessage) {
        this.specialMessage = specialMessage;
    }

    public IPAddress getDestinationIP() {
        return destinationIP;
    }

    public void setDestinationIP(IPAddress destinationIP) {
        this.destinationIP = destinationIP;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }
    public String getRouterRoute() {
        return routerRoute;
    }

    public void setRouterRoute(String routerRoute) {
        this.routerRoute = routerRoute;
    }






}
