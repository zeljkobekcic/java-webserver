package de.hhu.rechnernetze.javawebserver;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/******************************************************************************
 * This class' purpose it to accept incoming network connections and handle
 * those with creating an object of the HttpRequest class.
 *
 * @author Zeljko Bekcic
 * @version 1.0
 ******************************************************************************/
public final class WebServer implements Runnable {

    static final Logger logger = Logger.getLogger(WebServer.class.getName());

    private ServerSocket serverSocket;
    private MIMEType mimetype;

    /**************************************************************************
     * Instancing an WebServer who listens to the given port.
     *
     * @param  port  The port to what the WebServer will listen
     * @throws IOException If an IO-Error occurs this Exception will be thrown
     **************************************************************************/
    public WebServer(int port, MIMEType mimetype) throws IOException {
        serverSocket = new ServerSocket(port);

        if(mimetype == null){
            logger.log(Level.WARNING, "AN ILLEGAL MIMETYPE HAS BEEN PASSED");
            throw new IllegalArgumentException("Illegal MIMEType " + mimetype);
        }

        this.mimetype = mimetype;
    }

    /**************************************************************************
     * Running an instance of the WebServer in an own Thread, it will accept
     * connections and handle them with HttpRequest Objects, until you stop
     * the Thread manually.
     **************************************************************************/
    @Override
    public void run(){

        logger.log(Level.FINEST, "STARTING TO RUN THE WEBSERVER IN AN OWN THREAD");

        //Process HTTP service request in an infinite loop
        try {
            while (true){
                //Instancing an object to handle a single request.
                //Each of these objects will run in an own thread.
                Thread thread = new Thread(listenForConnection());
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**************************************************************************
     * Listens for an incoming connection and creates an HttpRequest object,
     * which can handle this connection.
     *
     * <p>Note that the created object ist not handling the connection. It can
     * handle it with running it an Thread.</p>
     *
     * @throws IOException If an IO-Error occurs this Exception will be thrown
     **************************************************************************/
    public HttpRequest listenForConnection() throws IOException{

        // NOTE: The serversocket will wait/block until he gets an incoming
        //connection he can accept.
        Socket socket = serverSocket.accept();
        logger.log(Level.INFO, "ACCEPTING INCOMING CONNECTION");

        // Instancing an HttpRequest object to handle the accepted connection
        HttpRequest httpRequest = new HttpRequest(socket, mimetype);
        return httpRequest;
    }
}



