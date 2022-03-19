package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;

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
            if(msg.getPayload() instanceof RegisterRequest)
                register(msg);
            else if(msg.getPayload() instanceof DeregisterRequest)
                deregister(msg);
            else if(msg.getPayload() instanceof HandoffRequest)
                handoffFish(msg);
        }
    }

    private void register(Message msg) {

        String id = "tank" + (cc.size()+1);
        System.out.println(id);
        java.net.InetSocketAddress client = msg.getSender();
        cc.add(id, client);
    }

    private void deregister(Message msg) {
        try {
            String index = (((DeregisterRequest) msg.getPayload()).getId());
            cc.remove(Integer.parseInt(index));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handoffFish(Message msg) {
        FishModel fish = ((HandoffRequest) msg.getPayload()).getFish();

        InetSocketAddress target;

        int index = cc.indexOf(msg.getSender());
        if(fish.getDirection() == Direction.RIGHT)
            target = cc.getRightNeighorOf(index);
        else
            target = cc.getLeftNeighorOf(index);

        ep.send(target, fish);
    }

    public static void main(String[] args) {

        Broker b = new Broker();
        b.broker();
    }

}
