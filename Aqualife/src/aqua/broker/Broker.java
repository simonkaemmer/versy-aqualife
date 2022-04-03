package aqua.broker;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.msgtypes.DeregisterRequest;
import aqua.common.msgtypes.HandoffRequest;
import aqua.common.msgtypes.RegisterRequest;
import aqua.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;


import javax.swing.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {

    Boolean stopRequested;
    ClientCollection<InetSocketAddress> clientCollection;
    Endpoint endpoint;
    ExecutorService pool;
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
                register(sender);
            } else if (payload instanceof DeregisterRequest) {
                deregister((DeregisterRequest) payload);
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

    public void register(InetSocketAddress sender) {
        this.reentrantReadWriteLock.readLock().lock();
        String clientId = "tank" + this.clientCollection.size();
        this.clientCollection.add(clientId, sender);
        this.endpoint.send(sender, new RegisterResponse(clientId));
        this.reentrantReadWriteLock.readLock().unlock();
    }

    public void deregister(DeregisterRequest payload) {
        this.reentrantReadWriteLock.readLock().lock();
        this.clientCollection.remove(this.clientCollection.indexOf(payload.getId()));
        this.reentrantReadWriteLock.readLock().unlock();
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