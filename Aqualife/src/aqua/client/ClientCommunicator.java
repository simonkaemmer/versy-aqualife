package aqua.client;

import java.net.InetSocketAddress;

import aqua.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.common.FishModel;
import aqua.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, InetSocketAddress neighbor) {
			endpoint.send(neighbor, new HandoffRequest(fish));
		}

		public void forwardToken(InetSocketAddress addr) {
			endpoint.send(addr, new Token());
		}

/*		public void giveBackToken(InetSocketAddress receiver) {
			endpoint.send(receiver, new Token());
		}*/
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof Token)
					tankModel.receiveToken();
					System.out.println(msg.getSender());

				if (msg.getPayload() instanceof NeighborUpdate) {
					NeighborUpdate neighborUpdate = (NeighborUpdate) msg.getPayload();
					if (neighborUpdate.getLeftNeighbor() != null)
						tankModel.leftNeighbor = neighborUpdate.getLeftNeighbor();
					if (neighborUpdate.getRightNeighbor() != null)
						tankModel.rightNeighbor = neighborUpdate.getRightNeighbor();

					System.out.println("left neighbour " + tankModel.leftNeighbor);
					System.out.println("right neighbour " + tankModel.rightNeighbor);
					System.out.println("_______________________________________________");
				}

/*				if (msg.getPayload() instanceof Token) {
					tankModel.receiveToken();
				}*/
			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}