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
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samsung
 */
public class Client {



    public static void main(String[] args)
    {
        Socket socket=null;
        ObjectInputStream input = null;
        ObjectOutputStream output=null;
        Scanner  scanner=new Scanner(System.in);
        EndDevice endDevice=null;

        
        try {
            socket = new Socket("localhost", 1234);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Connected to server");
        try {
            endDevice=(EndDevice)input.readObject();
            System.out.println("End Device is received ");
            System.out.println("IP :"+endDevice.getIp()+" GatewayIp "+endDevice.getGateway());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        while(true){
            System.out.println("Do you want to send message or" +
                    " receive message\n\"s\" for send and \"r\" For Receive\nPress \"e\" to exit");
            String ans=scanner.nextLine();
            if(ans.equals("s")){
                try {
                    new SendThread(socket,input,output,endDevice).t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else if(ans.equals("r")){
//                try {
//                    Packet msg= (Packet) input.readObject();
//                    System.out.println("The message is "+msg.getMessage());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                }
            }
            else{
                try {
                    output.writeObject("exit");
                    socket.close();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Tasks
         */
        /*
        1. Receive EndDevice configuration from server
        2. Receive active client list from server        
        3. for(int i=0;i<100;i++)
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
        18. Report average number of hops and drop rate
        */
    }
}
