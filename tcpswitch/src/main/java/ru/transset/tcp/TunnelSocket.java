/**
 * АО Транссеть
 * 
 * http://transset.ru
 */
package ru.transset.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;

import ru.funsys.avalanche.Messages;


/**
 * Класс 
 * https://github.com/xulubo/tcptrace/blob/master/src/main/java/com/quickplay/tcptrace/SocketTunnel.java
 * модифицирован под требования кластерного приложения
 * 
 * @author Валерий Лиховских
 *
 */
public class TunnelSocket {
	
	protected SocketChannel remoteChannel;
	protected SocketChannel localChannel;
	private String name;
	protected Logger logger;
	protected TunnelFunction function;
	protected int size; // размер буфера
	
	protected TunnelSocket(TunnelFunction function, SocketChannel local, SocketChannel remote) {
		this.function = function;
		this.localChannel = local;
		this.remoteChannel = remote;
		logger = function.getLogger();
		size = function.getSize();
		SocketAddress addr = this.localChannel.socket().getRemoteSocketAddress();
		if (addr instanceof InetSocketAddress) {
			this.name = ((InetSocketAddress)addr).getHostString();
		}
		else {
			this.name = addr.toString();		
		}
	}

	public SocketChannel getRemoteChannel() {
		return remoteChannel;
	}

	public SocketChannel getLocalChannel() {
		return localChannel;
	}

	public boolean isConnected() {
		return this.localChannel.isConnected() && this.remoteChannel.isConnected();
	}

	public void relay(SocketChannel channel) throws IOException  {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(size);
			int count = channel.read(buffer);
			if (count > 0) {
				buffer.flip();
				if (channel == this.localChannel){
					count = this.remoteChannel.write(buffer);
					if (logger != null)	{
						logger.debug("Request - " + channel.toString());
						logger.debug(new String(buffer.array(), 0, count));
					}
				} else {
					if (channel == this.remoteChannel) {
						count = this.localChannel.write(buffer);
						if (logger != null)	{
							logger.debug("Response - " + channel.toString());
							logger.debug(new String(buffer.array(), 0, count));
						}
					}
				}
				buffer.clear();
			} else {
				if (count == -1) {
					if (logger != null)	logger.debug(Messages.getMessage("SWTCH51I"));
					disconnect();
				}
			}
		} catch(IOException e) {
			if (logger != null) {
				if (channel == this.localChannel) logger.error("Request - " + channel.toString());
				else logger.error("Response - " + channel.toString());
				logger.error(Messages.getMessage("SWTCH50E"), e);
			} else {
				if (channel == this.localChannel) System.err.println("Request - " + channel.toString());
				else System.err.println("Response - " + channel.toString());
				e.printStackTrace();
			}
			disconnect();
			throw e;
		}
	}

	/**
	 * Закрыть соединения с сервером и клиентом  
	 */
	public void disconnect() {
		try {
			this.localChannel.close();
			this.remoteChannel.close();		
		} catch (IOException e) {
			if (logger != null) logger.error(Messages.getMessage("SWTCH52E"), e);
			else e.printStackTrace();
		}
	}

	/**
	 * Получить имя объекта
	 * 
	 * @return имя объекта
	 */
	public Object getName() {
		return name;
	}

}
