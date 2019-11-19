/**
 * АО Транссеть
 * 
 * http://transset.ru
 */
package ru.transset.tcp;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import ru.funsys.avalanche.Messages;

/**
 * Класс 
 * https://github.com/xulubo/tcptrace/blob/master/src/main/java/com/quickplay/tcptrace/ListenerService.java
 * модифицирован под требования кластерного приложения
 *
 * @author Валерий Лиховских
 */
public class TunnelFunction implements Runnable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5851646274685862484L;
	
	/**
	 * Журнал событий
	 */
	private Logger logger;
	
	/**
	 * Имя объекта
	 */
	private String name; 
	
	/**
	 * Имя машины
	 */
	private String host; 

	private int size = 4096;
	
	private Selector selector;
	private List<Tunnel> pending; 
	private Map<Integer, Tunnel> map; // слушатели локальных портов
	private Map<SelectableChannel, TunnelSocket> tunnelMap; // созданные туннели
	
	private boolean running;
	private Thread thread;

	
	public TunnelFunction() {
		running = false;
		thread = null;
		pending = new LinkedList<Tunnel>();
	}
	
	public void init() {
		
	}
	
	public void done() throws InterruptedException {
		this.running = false;
		selector.wakeup();
		// Закрываем захваченные ресурсы
		try {
			selector.close();
		}catch (Exception e) {
			if (logger == null) {
				e.printStackTrace();
			} else {
				logger.error(Messages.getMessage("SWTCH56E"), e);
			}
		}
		thread.interrupt();
		
		for (TunnelSocket tunnelSocket : tunnelMap.values()) {
			tunnelSocket.disconnect();
		}
		for (Tunnel tunnel : pending) {
			tunnel.close();
		}
		
	}
	
	public boolean addTunnel(String name, Tunnel tunnel)  {
		pending.add(tunnel);
		tunnel.setLogger(logger);
		return true;
	}
	
	@Override
	public void run() {
		running = true;
		try {
			selector = Selector.open();
		} catch (Exception e) {
			if (logger == null) {
				e.printStackTrace();
			} else {
				logger.error(Messages.getMessage("SWTCH57E"), e);
			}
		}
		map = new HashMap<Integer, Tunnel>();
		tunnelMap = new HashMap<SelectableChannel, TunnelSocket>();
		for(Tunnel tunnel : pending) {
			try {
				ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
				serverSocketChannel.socket().bind(new InetSocketAddress(tunnel.getPort()));
				serverSocketChannel.socket().setReuseAddress(true);
				serverSocketChannel.configureBlocking(false);
				map.put(serverSocketChannel.socket().getLocalPort(), tunnel);
				SelectionKey serverKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
				tunnel.setServerSelectionKey(serverKey);
				tunnel.setServerSocketChannel(serverSocketChannel);
			} catch (Exception e) {
				if (logger == null) {
					System.err.println(tunnel.getName());
					e.printStackTrace();
				} else {
					logger.error(Messages.getMessage("SWTCH54E", new Object[] {tunnel.getName()}), e);
				}
			}
		}
		try {
			startSelector();
		} catch (IOException e) {
			if (logger == null) {
				e.printStackTrace();
			} else {
				logger.error(Messages.getMessage("SWTCH55E"), e);
			}
		}
	}
	
	private void startSelector() throws IOException {
		while(running) {
			  int readyChannels = selector.select();
			  if(readyChannels == 0) continue;
			  Set<SelectionKey> selectedKeys = selector.selectedKeys();
			  Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
			  while(keyIterator.hasNext()) {
				    SelectionKey key = keyIterator.next();
				    if (!key.isValid()) {
				    	continue;
				    }
				    if (key.isWritable()) {
				        // a channel is ready for writing
				    	continue;
				    }
				    if (key.isConnectable()) {
				        // a connection was established with a remote server.
				    	continue;
				    }
				    if(key.isAcceptable()) {
				    	ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
				    	accept(serverSocketChannel);
				    } else {
				    	if (key.isReadable()) {
				    		TunnelSocket t = tunnelMap.get(key.channel());
				    		try {
				    			t.relay((SocketChannel) key.channel());
				    		} catch(IOException e) {
				    			tunnelMap.remove(t.getLocalChannel());
				    			tunnelMap.remove(t.getRemoteChannel());
				    			key.cancel();
				    		}
					    }
				    }
				    keyIterator.remove();
			  }
		}
	}

	private void accept(ServerSocketChannel serverSocketChannel) {
		try {
			SocketChannel socketChannel = serverSocketChannel.accept();
			Integer port = serverSocketChannel.socket().getLocalPort();
			Tunnel tunnel = map.get(port);
			socketChannel.configureBlocking(false);
			socketChannel.register(selector, SelectionKey.OP_READ);
			TunnelSocket t = createTunnel(socketChannel, tunnel, logger, size);
			t.getRemoteChannel().configureBlocking(false);
			t.getRemoteChannel().register(selector, SelectionKey.OP_READ);
			
			tunnelMap.put(socketChannel, t);
			tunnelMap.put(t.getRemoteChannel(), t);
		} catch (Exception e) {
			if (logger != null) logger.error(Messages.getMessage("SWTCH53E"), e);
			else e.printStackTrace();
		}
	}
	
	public TunnelSocket createTunnel(SocketChannel localeChannel, Tunnel tunnel, Logger logger, int size) throws IOException {
		SocketChannel remoteChannel = SocketChannel.open();
		remoteChannel.connect(tunnel.getRemoteAddress());
		TunnelSocket tunnelSocket;
		if ("kafka".equals(tunnel.getType())) {
			tunnelSocket = new TunnelKafka(this, localeChannel, remoteChannel);
		} else {
			tunnelSocket = new TunnelSocket(this, localeChannel, remoteChannel);
		}
		return tunnelSocket;
	}
	

	public void setSize(String size) {
		this.size = Integer.parseInt(size);
	}
	
	public boolean isRunning() {
		return running;
	}

	public String getName() {
		return name;
	}

	public void on() {
		if (name == null) name = "Tunnel";
		thread = new Thread(this, name);
		thread.start();
	}

	public void off() throws InterruptedException {
		done();
	}

	public int findLocalPort(String hostname, int port) {
		try { 
			Tunnel tunnel = findTunnel(hostname, port);
			if (tunnel == null) {
				InetAddress address = InetAddress.getByName(hostname);
				tunnel = findTunnel(address.getHostName(), port);
				if (tunnel == null) {
					tunnel = findTunnel(address.getHostAddress(), port);
					if (tunnel == null) {
						tunnel = findTunnel(address.getCanonicalHostName(), port);
					}
				}
			}
			return tunnel.getPort(); 
		} catch (Exception e) {
			return 0;
		}
	}
	
	private Tunnel findTunnel(String hostname, int port) {
		String strName = hostname.toUpperCase();
		String strPort = ":" + port;
		Tunnel result = null;
		for (int index = 0; index < pending.size() && result == null; index++) {
			Tunnel tunnel = pending.get(index);
			String remote = tunnel.getRemote();
			if (remote.startsWith(strName) && remote.endsWith(strPort)) {
				result = tunnel;
			}
		}
		return result;
	}
	
	public String getHostName() {
		String result;
		if (host == null) {
			try {
				result = InetAddress.getLocalHost().getHostName();
			} catch (Exception e) {
				result = null;
			}
			
		} else {
			result = host;
		}
		return result;
	}

	public int getSize() {
		return size;
	}
	
	public Logger getLogger() {
		return logger;
	}
	
}
