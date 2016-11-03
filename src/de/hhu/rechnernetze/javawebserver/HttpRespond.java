package de.hhu.rechnernetze.javawebserver;

import sun.rmi.runtime.Log;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by zeljko on 03.11.16.
 */
public class HttpRespond {

    private static final String CRLF = "\r\n";
    private final RequestType requestType;

    final static Logger logger = Logger.getLogger(HttpRespond.class.getName());

    String statusLine;
    String dateLine;
    String contentTypeLine;
    String contentLengthLine;
    String entityBody;

    public HttpRespond(Path pathToRequestedFile, RequestType requestType) {
        this.requestType = requestType;
        if(pathToRequestedFile == null) {
            logger.log(Level.WARNING, "RECEIVED ILLEGAL PATH : "
                    + pathToRequestedFile);
        }

        if(requestType == null) {
            logger.log(Level.WARNING, "RECEIVED ILLEGAL REQUEST TYPE: "
                    + requestType);
        }
    }

    private void initDateLine() {
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("EEE, dd. MMM yyyy HH:mm:ss ");
        dateLine = "Date: " + simpleDateFormat.format(new Date()) + CRLF;
    }

    private void initStatusLine(Path path) {

    }


    private boolean checkIfFileExists(Path path){
        File file = path.toFile();
        return file.isFile() && file.exists();
    }

}

