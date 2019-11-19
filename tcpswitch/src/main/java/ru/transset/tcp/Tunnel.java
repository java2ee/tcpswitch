/**
 * АО Транссеть
 * 
 * http://transset.ru
 */
package ru.transset.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.logging.log4j.Logger;

import ru.funsys.avalanche.Messages;

/**
 * Класс 
 * https://github.com/xulubo/tcptrace/blob/master/src/main/java/com/quickplay/tcptrace/Trace.java
 * модифицирован под требования кластерного приложения
 * 
 * @author Валерий Лиховских
 *
 */
public class Tunnel {
	
	private SelectionKey serverSelectionKey;
	private ServerSocketChannel serverSocketChannel;
	private final Vector<TunnelSocket> tunnels = new Vector<TunnelSocket>();
	
	private Logger logger;

	private int port;
	private InetSocketAddress remoteAddress;
	private String type;
	private String remote; 
	
	private String address;
	
	private String jmx;
	
	public int getPort() {
		return port;
	}
	
	public void setPort(String port) {
		this.port = Integer.parseInt(port);
	}
	
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	public void setType(String type) {
		this.type = type.toLowerCase();
	}
	
	public String getType() {
		return type;
	}
	
	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}
	
	public void setRemote(String remote) {
		StringTokenizer tokenizer = new StringTokenizer(remote, ":");
		if (tokenizer.hasMoreTokens()) {
			address = tokenizer.nextToken();
			if (tokenizer.hasMoreTokens()) {
				this.remoteAddress = new InetSocketAddress(address, Integer.parseInt(tokenizer.nextToken()));
				this.remote = remote.toUpperCase();
				return;
			}
		}
		if (logger == null) {
			System.out.println(Messages.getMessage("SWTCH58E", new Object[] {remote}));
		} else {
			logger.error(Messages.getMessage("SWTCH58E", new Object[] {remote}));
		}
		
		throw new NullPointerException();
	}

	public String getRemote() {
		return remote;
	}
	
	public String getName() {
		return port + " - " + remoteAddress.toString();	
	}
	
	public void disconnectAll() {
		Iterator<TunnelSocket> iter = tunnels.iterator();
		while(iter.hasNext()) {
			TunnelSocket t = iter.next();
			t.disconnect();
		}
	}
	
	public void close() {
		disconnectAll();
		if (serverSelectionKey != null) {
			try {
				serverSelectionKey.channel().close();
				serverSelectionKey.cancel();
				serverSocketChannel.socket().close();
				serverSocketChannel.close();
			} catch (IOException e) {
				if (logger == null) {
					System.out.println(Messages.getMessage("SWTCH59E"));
					e.printStackTrace();
				} else {
					logger.error(Messages.getMessage("SWTCH59E"), e);
				}
			}
		}
	}
	
	public SelectionKey getServerSelectionKey() {
		return serverSelectionKey;
	}

	void setServerSelectionKey(SelectionKey serverSelectionKey) {
		this.serverSelectionKey = serverSelectionKey;
	}

	void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
		this.serverSocketChannel = serverSocketChannel;
	}

	public void addTunnelSocket(TunnelSocket tunnel) {
		tunnels.add(tunnel);
	}
	
	public Vector<TunnelSocket> getTunnels() {
		return tunnels;
	}

	public void removeDisconnected() {
		Iterator<TunnelSocket> iter = tunnels.iterator();
		while(iter.hasNext()) {
			if (!iter.next().isConnected()) {
				iter.remove();
			}
		}
	}
	
	public void init() {
		
	}
	
	public void done() {
		
	}
	
}
