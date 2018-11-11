package networkLayer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by Afrina on 18-Nov-17.
 */
public class SocketStream {
    Socket socket;
    ObjectInputStream input;
    ObjectOutputStream output;
    SocketStream(Socket s,ObjectOutputStream oos,ObjectInputStream ois){
        socket=s;
        input=ois;
        output=oos;
    }

}
