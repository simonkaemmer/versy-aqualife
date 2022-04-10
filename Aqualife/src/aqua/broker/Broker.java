package aqua.broker;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;


import javax.swing.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {

    volatile Boolean stopRequested;
    ClientCollection<InetSocketAddress> clientCollection;
    Endpoint endpoint;
    ExecutorService pool;                                                         // Kerne / CPU-Intensität
    private static final int POOL_SIZE = (int) (Runtime.getRuntime().availableProcessors() / 0.5);
    ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();


    class BrokerTask implements Runnable {
        Message msg;

        private BrokerTask(Message msg) {
            System.out.println("Created Task");
            this.msg = msg;
        }

        @Override
        public void run() {
            Serializable payload = this.msg.getPayload();
            InetSocketAddress sender = this.msg.getSender();

            if (payload instanceof RegisterRequest) {
                register(msg);
            } else if (payload instanceof DeregisterRequest) {
                deregister(msg);
            } else if (payload instanceof HandoffRequest) {
                handoffFish((HandoffRequest) payload, sender);
            } else if (payload instanceof PoisonPill) {
                stopRequested = true;
            }
        }
    }

    class StopRequested implements Runnable {

        @Override
        public void run() {
            JOptionPane.showMessageDialog(null, "Press OK button to stop server!");
            stopRequested = true;
        }
    }

    public Broker() {
        this.stopRequested = false;
        this.clientCollection = new ClientCollection<>();
        this.endpoint = new Endpoint(4711);
        this.pool = Executors.newFixedThreadPool(POOL_SIZE);
        pool.execute(new StopRequested());
    }
    //Dispatcher
    public void broker() {
        Message msg;
        while (!this.stopRequested) {
            if ((msg = endpoint.nonBlockingReceive()) != null) {
                pool.execute(new BrokerTask(msg));
            }
        }
        pool.shutdown();
        System.out.println("Exited Broker");
    }

    public void register(Message message)  {
        InetSocketAddress sender = message.getSender();
        String clientId = "tank" + this.clientCollection.size();
        this.reentrantReadWriteLock.writeLock().lock();
        clientCollection.add(clientId, sender);
        this.reentrantReadWriteLock.writeLock().unlock();

   /*     if (clientCollection.size() == 1) {
            endpoint.send(sender, new Token());
        }*/

        InetSocketAddress leftNeighbor = clientCollection.getLeftNeighborOf(clientCollection.indexOf(sender));
        InetSocketAddress rightNeighbor = clientCollection.getRightNeighborOf(clientCollection.indexOf(sender));

        NeighbourUpdate senderNeighborUpdate = new NeighbourUpdate(clientId, leftNeighbor, rightNeighbor);
        NeighbourUpdate leftNeighborUpdate = new NeighbourUpdate("left", clientCollection.getLeftNeighborOf(clientCollection.indexOf(leftNeighbor)), sender);
        NeighbourUpdate rightNeighborUpdate = new NeighbourUpdate("right", sender, clientCollection.getRightNeighborOf(clientCollection.indexOf(rightNeighbor)));

        endpoint.send(sender, senderNeighborUpdate);
        endpoint.send(leftNeighbor, leftNeighborUpdate);
        endpoint.send(rightNeighbor, rightNeighborUpdate);
        endpoint.send(sender, new RegisterResponse(clientId));
    }

    public void deregister(Message message) {
        DeregisterRequest deregisterRequest = (DeregisterRequest) message.getPayload();
        String tankId = deregisterRequest.getId();
        this.reentrantReadWriteLock.readLock().lock();
        int tankIndex = clientCollection.indexOf(tankId);
        this.reentrantReadWriteLock.readLock().unlock();

        InetSocketAddress leftNeighbor = clientCollection.getLeftNeighborOf(clientCollection.indexOf(tankId));
        InetSocketAddress rightNeighbor = clientCollection.getRightNeighborOf(clientCollection.indexOf(tankId));

        NeighbourUpdate leftNeighborUpdate = new NeighbourUpdate("left", clientCollection.getLeftNeighborOf(clientCollection.indexOf(leftNeighbor)), rightNeighbor);
        NeighbourUpdate rightNeighborUpdate = new NeighbourUpdate("right", leftNeighbor, clientCollection.getRightNeighborOf(clientCollection.indexOf(rightNeighbor)));

        this.reentrantReadWriteLock.writeLock().lock();
        clientCollection.remove(tankIndex);
        this.reentrantReadWriteLock.writeLock().unlock();

        endpoint.send(leftNeighbor, leftNeighborUpdate);
        endpoint.send(rightNeighbor, rightNeighborUpdate);
    }


    public void handoffFish(HandoffRequest payload, InetSocketAddress sender) {
        FishModel fish = payload.getFish();
        this.reentrantReadWriteLock.readLock().lock();
        int indexOfSender = this.clientCollection.indexOf(sender);
        InetSocketAddress receiver = fish.getDirection() == Direction.RIGHT ? this.clientCollection.getRightNeighborOf(indexOfSender) : this.clientCollection.getLeftNeighborOf(indexOfSender);
        this.endpoint.send(receiver, payload);
        this.reentrantReadWriteLock.readLock().lock();
    }

    public static void main(String[] args) {
        new Broker().broker();
    }
}