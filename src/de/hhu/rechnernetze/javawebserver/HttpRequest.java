package de.hhu.rechnernetze.javawebserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    DataOutputStream dataOutputStream;
    BufferedReader bufferedReader;


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
        try {
            processHttpRequest();
        } catch (IOException e) {
            logger.log(Level.WARNING, "AN IO-ERROR OCCURRED WHILE PROCESSING" +
                    " THE HTTP REQUEST", e);
            e.printStackTrace();
        }
    }

    //checking if the file exists
    private boolean checkIfFileExists(Path path){
        File file = path.toFile();
        return file.isFile() && file.exists();
    }

    //processing the http request and then closing the streams.
    private void processHttpRequest() throws IOException {

        dataOutputStream = new DataOutputStream(socket.getOutputStream());
        bufferedReader =
                new BufferedReader(new InputStreamReader(socket.getInputStream()));

        respondToRequest();

        //closing the closeables
        dataOutputStream.close();
        bufferedReader.close();
        socket.close();
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
