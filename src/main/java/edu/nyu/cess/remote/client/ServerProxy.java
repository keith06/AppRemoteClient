/**
 *
 */
package edu.nyu.cess.remote.client;

import java.io.File;
import java.util.ArrayList;

import edu.nyu.cess.remote.common.app.ExecutionRequest;
import edu.nyu.cess.remote.common.app.State;
import edu.nyu.cess.remote.common.net.ClientNetworkInterface;
import edu.nyu.cess.remote.common.net.ClientNetworkInterfaceObserver;
import edu.nyu.cess.remote.common.net.DataPacket;
import edu.nyu.cess.remote.common.net.SocketInfo;

/**
 * @author akira
 */
public class ServerProxy implements ClientNetworkInterfaceObserver, ServerProxyObservable {

	private final ArrayList<ServerProxyObserver> observers = new ArrayList<ServerProxyObserver>();

	private static ClientNetworkInterface networkInterface;

	private final int POLL_INTERVAL = 2000; // miliseconds

	public ServerProxy(File serverLocation) {
		SocketInfo socketInfo = new SocketInfo();
		socketInfo.readFromFile(serverLocation);

		networkInterface = new ClientNetworkInterface(socketInfo);
		networkInterface.addObserver(this);

	}

	public void initPersistentServerConnection() {

		while (true) {
			networkInterface.pollServerUntilSocketInitialized(POLL_INTERVAL);
			networkInterface.readPacketsUntilSocketClosed();
			try {
				Thread.sleep(POLL_INTERVAL);
			} catch (InterruptedException e) {
			}
			System.out.println("attempting to reconnect to server...");
		}
	}

	public void updateApplicationState(State state) {
		networkInterface.writeDataPacket(new DataPacket(state));
	}

	public void addObserver(ServerProxyObserver observer) {
		observers.add(observer);
	}

	public void deleteObserver(ServerProxyObserver observer) {
		observers.remove(observer);
	}

	public void notifyApplicationExececutionRequestReceived(ExecutionRequest execRequest) {
		for (ServerProxyObserver observer : observers) {
			observer.execRequestUpdate(execRequest);
		}
	}

	public void notifyNetworkStatusUpdate(boolean isConnected) {
		for (ServerProxyObserver observer : observers) {
			observer.networkStatusUpdate(isConnected);
		}
	}

	public void notifyServerMessageReceived(String message) {
		for (ServerProxyObserver observer : observers) {
			observer.serverMessageUpdate(message);
		}
	}

	public void networkPacketUpdate(DataPacket dataPacket, String ipAddress) {
		System.out.println("Network Packet Received.");
		Object obj = dataPacket.getContent();
		if (obj != null) {
			if (obj instanceof ExecutionRequest) {
				System.out.println("Packet Content: ApplicationExecRequest");
				ExecutionRequest execReq = (ExecutionRequest) dataPacket.getContent();
				notifyApplicationExececutionRequestReceived(execReq);
			}
			else if (obj instanceof String) {
				System.out.println("String received from " + ipAddress);

				notifyServerMessageReceived(((String) obj));
			}
		}
	}

	public void networkStatusUpdate(String ipAddress, boolean isConnected) {
		notifyNetworkStatusUpdate(isConnected);
	}

}
