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
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

@ClientEndpoint
public class WebsocketClientEndpoint {
	
    private Session userSession = null;
    
    private final List<Consumer<String>> callbackConsumer;
    
    private final List<ReconnectHandler> reconnectHandler;
    
    private final CountDownLatch connectLatch = new CountDownLatch(1);
    
	private URI endpointURI;

	public WebsocketClientEndpoint(final URI endpointURI) {
		this.endpointURI = endpointURI;
		this.callbackConsumer = new ArrayList<>();
		this.reconnectHandler = new ArrayList<>();
	}
	
	public void connect() throws DeploymentException, IOException, InterruptedException {
	      final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
          container.connectToServer(this, endpointURI);

          connectLatch.await();
	}

    @OnOpen
    public void onOpen(final Session userSession) {
        System.out.println("opening websocket");
        this.userSession = userSession;
        connectLatch.countDown();
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("closing websocket: " + reason);
        this.userSession = null;
        reconnectHandler.forEach((h) -> h.handleReconnect());
    }
    
    @OnMessage
    public void onMessage(final String message) {
    		callbackConsumer.forEach((c) -> c.accept(message)); 
    }
    
    public void sendMessage(final String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }
    
    public void addConsumer(final Consumer<String> consumer) {
    		callbackConsumer.add(consumer);
    }
    
    public boolean removeConsumer(final Consumer<String> consumer) {
		return callbackConsumer.remove(consumer);
    }
    
    public void addReconnectHandler(final ReconnectHandler theReconnectHandler) {
    		reconnectHandler.add(theReconnectHandler);
    }

}
