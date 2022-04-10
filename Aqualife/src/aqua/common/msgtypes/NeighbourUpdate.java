//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package aqua.common.msgtypes;

import aqua.common.Direction;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class NeighbourUpdate implements Serializable {

	private InetSocketAddress left;
	private InetSocketAddress right;
	private String id;

	public NeighbourUpdate(String id, InetSocketAddress left, InetSocketAddress right) {
		this.id = id;
		this.left = left;
		this.right = right;
	}
	public String getId() {
		return id;
	}

	public InetSocketAddress getLeftNeighbor() {
		return left;
	}

	public InetSocketAddress getRightNeighbor() {
		return right;
	}
}