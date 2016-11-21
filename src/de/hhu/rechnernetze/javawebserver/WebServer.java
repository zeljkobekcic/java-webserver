package de.hhu.rechnernetze.javawebserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
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

    /***************************************************************************
     * Running the program.
     *
     * @param args The path to the mime.types file
     * @throws Exception
     **************************************************************************/
    public static void main(String[]args) throws Exception {

        MIMEType.logger.setLevel(Level.WARNING);
        WebServer.logger.setLevel(Level.WARNING);
        HttpRequest.logger.setLevel(Level.WARNING);

        if(args.length == 2 && args[0].equals("-mime")){

            MIMEType mimetype = new MIMEType(Paths.get(args[1]));

            //Setting port number and starting the server
            int port = 6789;
            WebServer webServer = new WebServer(port, mimetype);

            new Thread(webServer).start();

        } else {
            System.out.println("PLEASE SPECIFY AN MIME FILE WITH -mime <path/to/the/file>");
        }
    }
}

/******************************************************************************
 * The HttpRequest class will handle the received HTTP request within an own
 * Thread.
 *
 * @author Zeljko Bekcic
 * @version 1.0
 ******************************************************************************/
final class HttpRequest implements Runnable {

    final static String CRLF = "\r\n";
    final static Logger logger = Logger.getLogger(HttpRequest.class.getName());

    MIMEType mimeType;
    Socket socket;

    DataOutputStream dataOutputStream=null;
    BufferedReader bufferedReader=null;


    /**************************************************************************
     * Constructs an basic HttpRequest with the given socket.
     *
     * @param socket The socket which holds the connection to whom requested one.
     * @param mimeType Gives the mimeType for file-endings.
     * @throws IllegalArgumentException if the specified socket is null
     **************************************************************************/
    public HttpRequest(Socket socket, MIMEType mimeType) {
        if (socket == null) {
            logger.log(Level.WARNING, "RECEIVED ILLEGAL SOCKET : " + socket);
            throw new IllegalArgumentException("Illegal Socket: " + socket);
        }
        this.socket = socket;

        if (mimeType == null) {
            logger.log(Level.WARNING, "RECEIVED ILLEGAL MIMETYPE : " + socket);
            throw new IllegalArgumentException("Illegal MIMEType: " + socket);
        }

        this.mimeType = mimeType;
    }

    /**************************************************************************
     * Handling the HTTP request within this method
     **************************************************************************/
    @Override
    public void run() {
        processHttpRequest();
        close();
    }

    //closing the streams
    public void close() {
        try {
            dataOutputStream.close();
        } catch (IOException e) {
            System.err.println("AN ERROR OCCURRED WHILE CLOSING THE " +
                    "DATAOUTPUTSTREAM");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("THE DATAOUTPUTSTREAM HAS NOT BEEN CLOSED " +
                    "BECAUSE IT WAS NULL");
        }

        try {
            bufferedReader.close();
        } catch (IOException e) {
            System.err.println("AN ERROR OCCURRED WHILE CLOSING THE " +
                    "BUFFEREDREADER");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("THE BUFFEREDREADER HAS NOT BEEN CLOSED " +
                    "BECAUSE IT WAS NULL");
        }

        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("AN ERROR OCCURRED WHILE CLOSING THE SOCKET");
            e.printStackTrace();
        }
    }

    //checking if the file exists
    private boolean checkIfFileExists(Path path){
        File file = path.toFile();
        return file.isFile() && file.exists();
    }

    //processing the http request and then closing the streams.
    private void processHttpRequest() {
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            bufferedReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            respondToRequest();
        } catch( IOException e){
            System.err.println("AN ERROR OCCURRED WHILE PROCESSING THE HTTP " +
                    "REQUEST");
        }
    }

    //reading the file the user asked for
    //I am using this method only to get the byte size of the requested File.
    private String readFile(FileInputStream fileInputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int bytes = 0;

        String content = "";

        while ((bytes = fileInputStream.read(buffer)) != -1) {
            content += new String(buffer);
        }
        return content;
    }

    //sending the file safe
    //problems occurred if I have not used this method for sending the entity body
    private void sendBytes(FileInputStream fileInputStream) throws IOException {

        byte[] buffer = new byte[1024];
        int bytes = 0;

        while ((bytes = fileInputStream.read(buffer)) != -1) {
            dataOutputStream.write(buffer, 0, bytes);
        }
    }

    //getting the contentType for the file ending
    private String contentType(String fileName) {
        //if the file has no data type at the end it will pass nearly the
        //entire string and get me the default value
        int lastDelimiter = fileName.lastIndexOf('/') + 1;
        String file = fileName.substring(lastDelimiter);
        lastDelimiter = file.lastIndexOf('.') + 1;
        String fileEnding = file.substring((lastDelimiter));

        return mimeType.getMIMEType(fileEnding);
    }

    //responding to the request depending on the HTTP request method
    private void respondToRequest() throws IOException {

        String requestLine = bufferedReader.readLine();

        logger.log(Level.FINE, "REQUESTLINE :\t" + requestLine);

        StringTokenizer tokens = new StringTokenizer(requestLine);
        String method = tokens.nextToken();
        String fileName = tokens.nextToken();

        //appending the filename to a dot to prevent that the server thinks,
        //that the file is located at the root, the dot is for the current
        //directory.
        fileName = "." + fileName;

        logger.log(Level.FINER, "REQUEST METHOD :\t" + method);

        switch (method){
            case "GET":
                respondToGET(fileName);
                break;

            case "HEAD":
                respondToHEAD(fileName);
                break;

            case "POST":
                respondToPOST();
                break;

            default:
                respondToInvalid();
                break;
        }
    }

    //
    // RESPONDING TO REQUEST METHODS WITH MORE METHODS
    //
    private void respondToGET(String fileName) throws IOException {

        logger.log(Level.FINEST, "REQUESTED FILE IS :\t" + fileName);

        boolean fileExists = checkIfFileExists(Paths.get(fileName));

        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ");

        String statusLine = "";
        String contentType = "";
        String contentLength = "";
        String entityBody = "";
        String date = "Date: " + simpleDateFormat.format(new Date());

        String userAgent;

        //looping to the user agent, everything else is not relevant now.
        while(!(userAgent = bufferedReader.readLine())
                .toUpperCase().startsWith("USER-AGENT:"));

        logger.log(Level.FINEST, "THE REQUESTED FILE IS : " + fileName);

        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK";
            contentType = "Content-Type: " + contentType(fileName);
            entityBody = readFile(new FileInputStream(Paths.get(fileName).toFile()));

            logger.log(Level.FINEST, "FILE HAS BEEN FOUND");
        } else {
            statusLine = "HTTP/1.0 404 Not Found";
            contentType = "Content-type: " + contentType("htm");
            entityBody = "<HTML>\n" +
                    "    <HEAD>\n" +
                    "        <TITLE>\n" +
                    "            NOT FOUND" +
                    "        </TITLE>\n" +
                    "    </HEAD>\n" +
                    "    <BODY>\n" +
                    "       I COULD NOT FIND THE FILE YOUR WERE ASKING FOR" +
                    "<p>" +
                    "BUT I KNOW YOU IP-ADDRESS, WHICH IS :<b> " +
                    socket.getRemoteSocketAddress().toString() +
                    "</b></p><p>" +
                    "AND YOUR USER AGENT WHICH IS: <b>" +
                    //removing the user-agent: substring and trimming the
                    // string so that it looks nice
                    userAgent.substring(new String("USER-AGENT: ").length()).trim() +
                    "</b><br>" +
                    "</BODY>\n" +
                    "</HTML>";

        }

        contentLength = "Content-Length: " + entityBody.getBytes("UTF-8").length;

        //pushing these to you back
        dataOutputStream.writeBytes(statusLine + CRLF);
        dataOutputStream.writeBytes(date + CRLF);
        dataOutputStream.writeBytes(contentType + CRLF);
        dataOutputStream.writeBytes(contentLength + CRLF);
        dataOutputStream.writeBytes(CRLF);

        if (fileExists) {
            FileInputStream fileInputStream = new FileInputStream(
                    Paths.get(fileName).toFile()
            );
            sendBytes(fileInputStream);
            fileInputStream.close();
        } else {
            dataOutputStream.writeBytes(entityBody);
        }
    }

    private void respondToHEAD(String fileName) throws IOException {

        logger.log(Level.FINEST, "REQUESTED FILE IS :\t" + fileName);

        boolean fileExists = checkIfFileExists(Paths.get(fileName));

        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ");

        String statusLine = "";
        String contentType = "";
        String entityBody = "";
        String contentLength = "";
        String date = "Date: " + simpleDateFormat.format(new Date());

        logger.log(Level.FINEST, "THE REQUESTED FILE IS : " + fileName);

        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK";
            contentType = "Content-type: " + contentType(fileName);
            logger.log(Level.FINEST, "FILE HAS BEEN FOUND");
            entityBody = readFile(new FileInputStream(Paths.get(fileName).toFile()));
        } else {
            statusLine = "HTTP/1.0 404 Not Found";
            contentType = "Content-type: " + contentType("htm");
            contentLength = "Content-Length: " + entityBody.getBytes("UTF-8").length;
            logger.log(Level.FINEST, "FILE NOT FOUND");
        }

        contentLength = "Content-Length: " + entityBody.getBytes("UTF-8").length;

        dataOutputStream.writeBytes(statusLine + CRLF);
        dataOutputStream.writeBytes(date + CRLF);
        dataOutputStream.writeBytes(contentType + CRLF);
        dataOutputStream.writeBytes(contentLength + CRLF);
        dataOutputStream.writeBytes(CRLF);
    }

    private void respondToPOST() throws IOException {

        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ");

        String statusLine = "HTTP/1.0 501 NOT IMPLEMENTED YET";
        String contentType = "Content-type: " + contentType("htm");
        String date = "Date: " + simpleDateFormat.format(new Date());
        String entityBody = "<HTML>" +
                "<HEAD><TITLE>NOT IMPLEMENTED YEY</TITLE></HEAD>" +
                "<BODY>I COULD NOT FIND THE FILE YOU WERE ASKING FOR</BODY></HTML>";
        String contentLength = "Content-Length: " + entityBody.getBytes("UTF-8").length;

        dataOutputStream.writeBytes(statusLine + CRLF);
        dataOutputStream.writeBytes(date + CRLF);
        dataOutputStream.writeBytes(contentType + CRLF);
        dataOutputStream.writeBytes(contentLength + CRLF);
        dataOutputStream.writeBytes(CRLF);
        dataOutputStream.writeBytes(entityBody);

    }

    private void respondToInvalid() throws IOException {

        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("'Date:' EEE, dd. MMM yyyy HH:mm:ss z");

        String statusLine = "HTTP/1.0 400 BAD REQUEST";
        String contentType = "Content-type: " + contentType("htm");
        String date = "Date: " + simpleDateFormat.format(new Date());
        String entityBody = "<HTML>" +
                "<HEAD><TITLE>BAD REQUEST</TITLE></HEAD>" +
                "<BODY>I DON'T KNOW WHAT YOU WANT ME TO DO</BODY></HTML>";
        String contentLength = "Content-Length: " + entityBody.getBytes("UTF-8").length;

        dataOutputStream.writeBytes(statusLine + CRLF);
        dataOutputStream.writeBytes(date + CRLF);
        dataOutputStream.writeBytes(contentType + CRLF);
        dataOutputStream.writeBytes(contentLength + CRLF);
        dataOutputStream.writeBytes(CRLF);
        dataOutputStream.writeBytes(entityBody);
    }
}

/******************************************************************************
 * This class is wrapping a HashMap<String, String> and which is automatically
 * filled with the given values from the given file.
 *
 * @author Zeljko Bekcic
 * @version 1.0
 ******************************************************************************/
class MIMEType {
    private final HashMap<String, String> mimeTypes = new HashMap<>();

    static final Logger logger = Logger.getLogger(MIMEType.class.getName());

    /**************************************************************************
     * This creates an instance of the MIMEType class which pulls then the mime
     * types from the Path.
     *
     * <p>Note that if the <i>path<i> is null or points to an directory you
     * will get an IllegalArgumentException, because these two cases lead to
     * an invalid object state.</p>
     *
     * @param path To the MIME Type file.
     **************************************************************************/
    public MIMEType(Path path) throws IOException {

        //
        // catching arguments which would lead to an illegal object state.
        //

        //If the argument is just wrong.
        if(path == null) {
            logger.log(Level.INFO, "THE PROVIDED PATH IS NOT VALID");
            throw new IllegalArgumentException("Illegal Path " + path);
        }

        File file = new File(path.toString());

        //If you pass a path to a directory you will get an Exception
        if(!file.isFile()) {
            logger.log(Level.SEVERE, "THE PROVIDED PATH TO THE FILE IS NOT A FILE");
            throw new IllegalArgumentException("Illegal Path " + path +
                    " The path is not pointing to a file.");
        }



        logger.log(Level.FINEST, "STARTING PARSING DATA FROM THE PROVIDED FILE : " + path.toString());

        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;

        while((line = bufferedReader.readLine()) != null){
            //Skipping the commented lines with line.contains("#")
            if(line.length() != 0 && !line.contains("#")){
                loadMIMETypeToHashMap(line);
            }
        }

        logger.log(Level.INFO, "FINISHED PARSING DATA FROM THE GIVEN FILE SUCCESSFUL");

        fileReader.close();
        bufferedReader.close();
    }

    /**************************************************************************
     * Returns the top-level MIME-Type for the given data ending.
     *
     * @param dataEnding The data ending you want the MIME-Type for.
     * @return Either the MIME-Type for the file ending you passed to the
     * method or the default value, which is <i>application/octet-stream</i>
     **************************************************************************/
    public String getMIMEType(String dataEnding){
        return mimeTypes.getOrDefault(dataEnding, "application/octet-stream");
    }


    //This method receives a MIME Type String (one single line) and puts it in a
    //HashMap. If the given MIME Type has no parameters (consists only of the
    //top-level type and the subtype name), then this MIME Type will not be
    //added to the HashMap.
    private void loadMIMETypeToHashMap(String mimeTypeString){
        //Splitting the given String into the type and parameters
        StringTokenizer stringTokenizer = new StringTokenizer(mimeTypeString);

        //only splitt if the line has there are two or more token, otherwise
        //there is no key for the type.
        if(stringTokenizer.countTokens()>=2){
            String type = stringTokenizer.nextToken();
            String token = null;
            while(stringTokenizer.hasMoreTokens()){
                token = stringTokenizer.nextToken();
                logger.info("MAPPING VALUE : " + type + " TO KEY : " + token);
                mimeTypes.put(token, type);
            }

        }
    }

}



