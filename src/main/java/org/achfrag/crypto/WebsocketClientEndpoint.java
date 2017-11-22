package org.achfrag.crypto;

import java.io.IOException;
import java.net.URI;

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

	public WebsocketClientEndpoint(final URI endpointURI) throws DeploymentException, IOException {
	      WebSocketContainer container = ContainerProvider.getWebSocketContainer();
          container.connectToServer(this, endpointURI);
	}

    @OnOpen
    public void onOpen(final Session userSession) {
        System.out.println("opening websocket");
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("closing websocket");
        this.userSession = null;
    }
    
    @OnMessage
    public void onMessage(final String message) {
    		System.out.println("New message: " + message);
    }
    
    public void sendMessage(final String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

}
