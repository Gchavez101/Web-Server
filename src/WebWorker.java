/**
 *
 * Web worker: an object of this class executes in its own new thread
 * to receive and respond to a single HTTP request. After the constructor
 * the object executes on its "run" method, and leaves when it is done.
 **/

import java.net.*;
import java.lang.*;
import java.io.*;
import java.util.*;
import java.text.*;
import javax.activation.*;



public class WebWorker implements Runnable {

  private Socket socket;
  private boolean fileExists = false;
  private DataOutputStream os;
  private FileInputStream fis;
  private String fileName;
  static String fileType;
  static String type;
  static String contentTypeLine = null;

  /**
   * Constructor: must have a valid open socket
   **/
  public WebWorker(Socket s) {
    socket = s;
  }




  /**
   * Worker thread starting point. Each worker handles just one HTTP
   * request and then returns, which destroys the thread. This method
   * assumes that whoever created the worker created it with a valid
   * open socket object.
   **/
  public void run() {
    System.err.println("Handling connection...");
    try {
      //Gets a reference to the socket's input and output stream
      InputStream is = socket.getInputStream();
      os = new DataOutputStream(socket.getOutputStream());
      readHTTPRequest(is);
      writeHTTPHeader(os, type);
      writeContent(os);
      os.flush();
      socket.close();
      fis.close();
    } catch (Exception e) {
      System.err.println("Output error: " + e);
    }
    System.err.println("Done handling connection.");
    return;
  }

  /**
   * Read the HTTP request header.
   * Save name of file request and check for existance
   **/
  private void readHTTPRequest(InputStream is) {

    /*
     * read in server requests
     * first read contains the name of page requested, save that name
     * search of file exists with that name, if not, set fileExists to false
     * continue with proccessing information until no more is obtained
     */
    //String Variable 
    String line;

    //This sets up the input stream filter
    BufferedReader r = new BufferedReader(new InputStreamReader(is));
    int check = 1;
    while (true) {
      try {
        while (!r.ready()) Thread.sleep(1);
        //Gets the request line of the HTTP Request message
        line = r.readLine();

        /*
         *tokenize lines and save the name of file as a string
         */
        if (check > 0) {
          StringTokenizer tokens = new StringTokenizer(line);
          tokens.nextToken();
          fileName = tokens.nextToken();

          //Prepand a "." so that the file request is within the directory we are working with
          fileName = "." + fileName;

          /* MimetypesFileTypeMap doesn't read or know what .ico files are 
           * so If statement is used to fix that problem. MimeTypes finds the fileType and sets it accordingly.
           */
          if (fileName.endsWith(".ico")) {
            fileType = "image/x-icon";
          }
          if (fileName.endsWith(".png")){
            fileType = "image/png";
          }else {
            MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap();
            fileType = mimeMap.getContentType(fileName);
          }
          type = fileType.split("/")[0];

          /*
           * TESTING PURPOSES
           */
          System.out.println("*****************");
          System.out.println(fileType);
          System.out.println("*****************");

          fis = null;
          fileExists = true;

          /*
           *If the file exists, continue,
           *else set fileExists to false as a check for later
           */
          try {
            fis = new FileInputStream(fileName);
          } catch (FileNotFoundException e) {
            fileExists = false;
          } //end of try and catch
          check--;
        }
        /*
         *print to terminal for debugging purposes
         */
        //This line keeps showing hte line of code that the processor is throwing at you
        System.err.println("Request line: (" + line + ")"); 
        if (line.length() == 0) break;
      } catch (Exception e) {
        System.err.println("Request error: " + e);
        break;
      }
    }
    return;
  }

  /**
   * Write the HTTP header lines to the client network connection.
   * @param os is the OutputStream object to write to
   * @param contentType is the string MIME content type (e.g. "text/html")
   **/
  private void writeHTTPHeader(OutputStream os, String contentType) throws Exception {
    /*
     * change page status depending if the fie requested exists or not
     * include information to be added in web page
     */
    if (fileExists) {
      os.write("HTTP/1.1 200 OK\n".getBytes());
      os.write(type.getBytes());
    } else {
      os.write("HTTP/1.1 404 Not Found\n".getBytes());
      os.write(type.getBytes());

    }

    Date d = new Date();
    DateFormat df = DateFormat.getDateTimeInstance();
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    os.write("HTTP/1.1 200 OK\n".getBytes());
    os.write("Date: ".getBytes());
    os.write((df.format(d)).getBytes());
    os.write("\n".getBytes());
    os.write("Server: Gonzalo's very own server\n".getBytes());
    //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
    //os.write("Content-Length: 438\n".getBytes()); 
    os.write("Connection: close\n".getBytes());
    os.write("Content-Type: ".getBytes());
    os.write(contentType.getBytes());
    os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
    return;
  }

  /**
   * Write the HTML file content.
   * @param fis is the fileInput that is being used
   * @param os is the output stream
   */

  private static void fileInput(FileInputStream fis, OutputStream os) throws Exception {
    /*
     * if file exists, create a buffer that reads the requested page
     * if the tags are found, replace them with varaibles
     * else if file doesnt exists, create 404 not found page
     */
    byte[] buffer = new byte[1024];
    int bytes = 0;
    Date date = new Date();
    String fileContents;
    String server = "GC Server";
    InetAddress ip = InetAddress.getLocalHost();

    /*
     *if file requests is an image, send byte as is
     *else check byte for tags
     */
    if (type.equals("image")) {
      while ((bytes = fis.read(buffer)) != -1) {
        os.write(buffer, 0, bytes);
      }
    } else {
      /* EXTRA CREDIT:
       *Loads the Favicon.ico
       */
      os.write("<html><head><title>GC Server</title><link rel=\"icon\" href=\"/test/favicon.ico\" type=\"image/x-icon\"/><link rel=\"shortcut icon\" href=\"/test/favicon.ico\" type=\"image/x-icon\"/></head>".getBytes());

      while ((bytes = fis.read(buffer)) != -1) {
        fileContents = new String(buffer, 0, bytes);
        //System.out.println("Buffer line: " + fileContents); //debugging purposes
        if (fileContents.contains("<cs371date>")) {
          fileContents = fileContents.replace("<cs371date>", date.toString());
        }
        if (fileContents.contains("<cs371server>")) {
          fileContents = fileContents.replace("<cs371server>", server);
        }
        //Just for Extra and it tells you the ip address you're using.
        if(fileContents.contains("<ip>")){
           fileContents = fileContents.replace("<cs371server>", ip.toString());
        }


        //System.out.println(fileContents); DEBUGGING PURPOSES
        buffer = fileContents.getBytes();
        //System.out.println(buffer); DEBUGGING PURPOSES
        os.write(buffer, 0, buffer.length);

      } //end of while
    } //ends else
  } //end of fileInput


  /**
   * Write the data content to the client network connection. This MUST
   * be done after the HTTP header has been written out.
   * @param os is the OutputStream object to write to
   **/
  private void writeContent(OutputStream os) throws Exception {
    if (fileExists) {
      fileInput(fis, os);
    } else {
      os.write("<html><head><title>GC Server</title><link rel=\"icon\" href=\"/test/favicon.png\" type=\"image/png\"/><link rel=\"shortcut icon\" href=\"/test/favicon.png\" type=\"image/png\"/></head><body>".getBytes());
      os.write("<h3>HTTP/1.1 404 Not Found\n</h3>\n".getBytes());
      os.write("</html></head></body>".getBytes());

    }
  }

} // end class