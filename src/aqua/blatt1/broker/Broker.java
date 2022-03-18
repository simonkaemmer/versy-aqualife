package aqua.blatt1.broker;

import messaging.Endpoint;
import messaging.Message;

public class Broker {

    private Endpoint ep;
    private ClientCollection<java.net.InetSocketAddress> cc;

    public Broker() {
        ep = new Endpoint(4711);
        cc = new ClientCollection<>();
    }

    public void broker() {
        while(true){
            Message msg = ep.blockingReceive();
        }
    }

    public void register() {
        String id = "tank" + cc.size();

    }

}
