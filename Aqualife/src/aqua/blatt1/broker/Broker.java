package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Broker {

    ClientCollection<InetSocketAddress> clientCollection;
    Endpoint endpoint;

    public Broker () {
        this.clientCollection = new ClientCollection<>();
        this.endpoint = new Endpoint(4711);
    }

    public void broker () {
        while (true) {
            Message msg = this.endpoint.blockingReceive();
            Serializable payload = msg.getPayload();

            if (payload instanceof RegisterRequest) {
                register(msg.getSender());
            } else if (payload instanceof DeregisterRequest) {
                deregister((DeregisterRequest) payload);
            } else if (payload instanceof HandoffRequest) {
                handoffFish((HandoffRequest) payload, msg.getSender());
            }
        }
    }

    public void register (InetSocketAddress sender) {
        String clientId = "tank" + this.clientCollection.size();
        this.clientCollection.add(clientId, sender);
        this.endpoint.send(sender, new RegisterResponse(clientId));

        System.out.println(clientId);
    }

    public void deregister (DeregisterRequest payload) {
        this.clientCollection.remove(this.clientCollection.indexOf(payload.getId()));
    }

    public void handoffFish(HandoffRequest payload, InetSocketAddress sender) {
        FishModel fish = payload.getFish();
        int indexOfSender = this.clientCollection.indexOf(sender);
        InetSocketAddress receiver = fish.getDirection() == Direction.RIGHT ? this.clientCollection.getRightNeighborOf(indexOfSender) : this.clientCollection.getLeftNeighborOf(indexOfSender);
        this.endpoint.send(receiver, payload);
    }

    public static void main(String[] args) {
        new Broker().broker();
    }
}