package de.hhu.rechnernetze.javawebserver;

import java.nio.file.Path;

/**
 * Created by zeljko on 03.11.16.
 */
public class HttpRespondBuilder {

    private RequestType requestType = null;
    private String fileName = null;

    public void setRequestType(RequestType requestType) {
       this.requestType = requestType;
    }

    public void setRequestedFile(Path path){
        fileName = path.getFileName().toString();

    }

}

enum RequestType {
    GET,
    POST,
    HEAD
}