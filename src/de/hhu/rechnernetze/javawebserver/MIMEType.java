package de.hhu.rechnernetze.javawebserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

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