package aqua.blatt1.broker;

import messaging.Endpoint;
import

public class Broker {

    private Endpoint ep;

    public Broker() {
        ep = new Endpoint(4711);
    }

}
