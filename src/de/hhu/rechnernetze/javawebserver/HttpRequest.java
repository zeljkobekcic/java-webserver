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

    private void sendBytes(FileInputStream fileInputStream) throws IOException {

        byte[] buffer = new byte[1024];
        int bytes = 0;

        StringBuilder stringBuilder = new StringBuilder();


        while ((bytes = fileInputStream.read(buffer)) != -1) {
            System.out.println(buffer.toString());
            stringBuilder.append(buffer);
            System.out.println(bytes);
            dataOutputStream.write(buffer, 0, bytes);
        }
        System.out.println(stringBuilder.toString());
    }

    private String contentType(String fileName) {
        //if the file has no data type at the end it will pass nearly the
        //entire string and get me the default value
        int lastDelimiter = fileName.lastIndexOf('/') + 1;
        String file = fileName.substring(lastDelimiter);
        lastDelimiter = file.lastIndexOf('.') + 1;
        String fileEnding = file.substring((lastDelimiter));

         return mimeType.getMIMEType(fileEnding);
    }

    private void respondToRequest() throws IOException {
        String requestLine = bufferedReader.readLine();

        logger.log(Level.FINEST, "REQUESTLINE :\t" + requestLine);

        StringTokenizer tokens = new StringTokenizer(requestLine);
        String method = tokens.nextToken();
        String fileName = tokens.nextToken();

        //appending the filename to a dot to prevent that the server thinks,
        //that the file is located at the root, the dot is for the current
        //directory.
        fileName = "." + fileName;

        logger.log(Level.FINEST, "REQUEST METHOD :\t" + method);

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
    // RESPONDING TO REQUEST METHODS
    //
    private void respondToGET(String fileName) throws IOException {

        logger.log(Level.FINEST, "REQUESTED FILE IS :\t" + fileName);

        boolean fileExists = checkIfFileExists(Paths.get(fileName));

        String statusLine;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd. MMM yyyy HH:mm:ss ");
        String date = "Date: " + simpleDateFormat.format(new Date()) + CRLF;

        String contentTypeLine;
        String entityBody = null;

        String userAgent;
        while(!(userAgent = bufferedReader.readLine()).toUpperCase().startsWith("USER-AGENT")){
            System.out.println(userAgent);
        }

        logger.log(Level.FINEST, "THE REQUESTED FILE IS : " + fileName);

        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK" + CRLF;

            contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
            logger.log(Level.FINEST, "FILE HAS BEEN FOUND");
        } else {
            statusLine = "HTTP/1.0 404 Not Found" + CRLF;
            contentTypeLine = "Content-type: " + contentType("htm") + CRLF;
            entityBody = "<HTML>" +
                    "<HEAD><TITLE>NOT FOUND</TITLE></HEAD>" +
                    "<BODY>I COULD NOT FIND THE FILE YOU WERE ASKING FOR<br>BUT" +
                    "I KNOW YOU IP-ADDRESS WHICH IS "+
                    socket.getRemoteSocketAddress().toString() + "<br>"
                    + userAgent + "<br>" + "</BODY></HTML>";
        }

        String contentLength = "Content-Length: " +
                contentTypeLine.getBytes("UTF-8").length + CRLF;

        dataOutputStream.writeBytes(statusLine);
        dataOutputStream.writeBytes(date);
        dataOutputStream.writeBytes(contentTypeLine);
        dataOutputStream.writeBytes(contentLength);
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


        System.out.println(fileName);

        logger.log(Level.FINEST, "REQUESTED FILE IS :\t" + fileName);

        boolean fileExists = checkIfFileExists(Paths.get(fileName));

        String statusLine;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'Date:' EEE, dd. MMM yyyy HH:mm:ss z");
        String date = simpleDateFormat.format(new Date()) + CRLF;

        String contentTypeLine;

        logger.log(Level.FINEST, "THE REQUESTED FILE IS : " + fileName);

        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK" + CRLF;
            contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
            logger.log(Level.FINEST, "FILE HAS BEEN FOUND");
        } else {
            statusLine = "HTTP/1.0 404 Not Found" + CRLF;
            contentTypeLine = "Content-type: " + contentType("htm") + CRLF;
            logger.log(Level.FINEST, "FILE NOT FOUND");
        }

        dataOutputStream.writeBytes(statusLine);
        dataOutputStream.writeBytes(date);
        dataOutputStream.writeBytes(contentTypeLine);
        dataOutputStream.writeBytes(CRLF);
    }

    private void respondToPOST() throws IOException {
        String statusLine = "HTTP/1.0 501 NOT IMPLEMENTED YET";
        String contentTypeLine = "Content-type: " + contentType("htm") + CRLF;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'Date:' EEE, dd. MMM yyyy HH:mm:ss z");
        String date = simpleDateFormat.format(new Date()) + CRLF;

        String entityBody = "<HTML>" +
                "<HEAD><TITLE>NOT IMPLEMENTED YEY</TITLE></HEAD>" +
                "<BODY>I COULD NOT FIND THE FILE YOU WERE ASKING FOR</BODY></HTML>";

        dataOutputStream.writeBytes(statusLine);
        dataOutputStream.writeBytes(date);
        dataOutputStream.writeBytes(contentTypeLine);
        dataOutputStream.writeBytes(CRLF);
        dataOutputStream.writeBytes(entityBody);

    }

    private void respondToInvalid() throws IOException {
        String statusLine = "HTTP/1.0 400 BAD REQUEST";
        String contentTypeLine = "Content-type: " + contentType("htm") + CRLF;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'Date:' EEE, dd. MMM yyyy HH:mm:ss z");
        String date = simpleDateFormat.format(new Date()) + CRLF;

        String entityBody = "<HTML>" +
                "<HEAD><TITLE>BAD REQUEST</TITLE></HEAD>" +
                "<BODY>I DON'T KNOW WHAT YOU WANT ME TO DO</BODY></HTML>";

        dataOutputStream.writeBytes(statusLine);
        dataOutputStream.writeBytes(date);
        dataOutputStream.writeBytes(contentTypeLine);
        //@TODO implement sending the size in bytes of the entityBody
        //dataOutputStream.writeBytes();
        dataOutputStream.writeBytes(CRLF);
        dataOutputStream.writeBytes(entityBody);
    }

    //
    //
    //
    private boolean checkIfFileExists(Path path){
        File file = path.toFile();
        return file.isFile() && file.exists();
    }
}