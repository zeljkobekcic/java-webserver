package de.hhu.rechnernetze.javawebserver;

import java.nio.file.Paths;
import java.util.logging.Level;

/******************************************************************************
 * The Main class' only serves to launch this program.
 *
 * @author Zeljko Bekcic
 * @version 1.0
 ******************************************************************************/
class Main {

    public static void main(String[]args) throws Exception {


        MIMEType.logger.setLevel(Level.WARNING);
        WebServer.logger.setLevel(Level.FINEST);
        HttpRequest.logger.setLevel(Level.FINEST);

        if(args.length == 2 && args[0].equals("-mime")){

            MIMEType mimetype = new MIMEType(Paths.get(args[1]));

            //Setting port number and starting the server
            int port = 6789;
            WebServer webServer = new WebServer(port, mimetype);

            new Thread(webServer).start();

        } else {
            System.out.println("PLEASE USE THE SPECIFY AN MIME FILE WITH -mime <path/to/the/file>");
        }

    }
}