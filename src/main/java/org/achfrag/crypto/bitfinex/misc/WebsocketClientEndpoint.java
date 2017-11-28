package org.achfrag.crypto.bitfinex.misc;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.achfrag.crypto.bitfinex.BitfinexApiBroker;
import org.achfrag.crypto.util.CloseableHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

@ClientEndpoint
public class WebsocketClientEndpoint {

	private Session userSession = null;

	private final List<Consumer<String>> callbackConsumer;

	private final List<WebsocketCloseHandler> closeHandler;

	private final CountDownLatch connectLatch = new CountDownLatch(1);

	private URI endpointURI;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(BitfinexApiBroker.class);


	public WebsocketClientEndpoint(final URI endpointURI) {
		this.endpointURI = endpointURI;
		this.callbackConsumer = new ArrayList<>();
		this.closeHandler = new ArrayList<>();
	}

	public void connect() throws DeploymentException, IOException, InterruptedException {
		final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		this.userSession = container.connectToServer(this, endpointURI);
		connectLatch.await();
	}

	@OnOpen
	public void onOpen(final Session userSession) {
		logger.info("Websocket is now open");
		connectLatch.countDown();
	}

	@OnClose
	public void onClose(final Session userSession, final CloseReason reason) {
		logger.info("Closing websocket: " + reason);
		closeHandler.forEach((h) -> h.handleWebsocketClose());
		this.userSession = null;
	}

	@OnMessage
	public void onMessage(final String message) {
		callbackConsumer.forEach((c) -> c.accept(message));
	}
	
	@OnError
    public void onError(final Session session, final Throwable t) {
        logger.error("OnError called {}", Throwables.getStackTraceAsString(t));
    }

	public void sendMessage(final String message) {
		
		if(userSession == null) {
			logger.error("Unable to send message, user session is null");
			return;
		}
		
		if(userSession.getAsyncRemote() == null) {
			logger.error("Unable to send message, async remote is null");
			return;
		}
		
		userSession.getAsyncRemote().sendText(message);
	}

	public void addConsumer(final Consumer<String> consumer) {
		callbackConsumer.add(consumer);
	}

	public boolean removeConsumer(final Consumer<String> consumer) {
		return callbackConsumer.remove(consumer);
	}

	public void addCloseHandler(final WebsocketCloseHandler theCloseHandler) {
		closeHandler.add(theCloseHandler);
	}

	public void close() {
		CloseableHelper.closeWithoutException(userSession);
		userSession = null;
	}

}
