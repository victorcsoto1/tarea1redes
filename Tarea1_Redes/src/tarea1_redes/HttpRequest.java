package tarea1_redes;

import java.io.*;
import java.net.*;
import java.util.*;

public final class HttpRequest implements Runnable {
 
    final static String CRLF = "\r\n";
    Socket socket;

    // Constructor
    public HttpRequest(Socket socket) throws Exception
    {
       this.socket = socket;
    }

    public void run()
    {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void processRequest() throws Exception
    {
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int ch;

        //Escritura del request message al stream del socket
        while((ch = is.read()) != -1) {
            outputStream.write(ch);
            if (ch == '\n') {
                ch = is.read();
                if (ch == '\n' || ch == '\r')
                    break;
                outputStream.write(ch);
            }
        }
        
        //Guarda el request message.
        String headerRequest = new String(outputStream.toByteArray(), "UTF-8");

        // Seteando inputs.
  
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        //String tokens para extraer el nombre del archivo y el metodo (GET o POST)
        StringTokenizer tokens = new StringTokenizer(headerRequest);
        String method = tokens.nextToken();  //Se obtiene el metodo
        String fileName = tokens.nextToken();  //Se obtiene el nombre del archivo
        //Se antepone un punto para que el archivo lo encuentre en el directorio actual.
        fileName = "." + fileName;
        
        if(method.equals("POST")){
            outputStream = new ByteArrayOutputStream();
            ch = is.read();
            while((ch = is.read()) != -1) {
                outputStream.write(ch);
                if (ch == '\n' || ch == '\r' || ch == '4'){
                    break;
                }
            }
            String payload = new String(outputStream.toByteArray(), "UTF-8");
            //puedes imprimirla si quieres:
            System.out.println(payload);
        }
        
        

        // Se abre el archivo.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
        }   

        //Construcción del mensaje de respuesta http.
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        if (fileExists) {
            statusLine = "HTTP/1.1 200 OK: ";
            contentTypeLine = "Content-Type: " +
            contentType(fileName) + CRLF;
        }else{
            statusLine = "HTTP/1.1 404 Not Found: ";
            contentTypeLine = "Content-Type: text/html" + CRLF;
            entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" + "<BODY>PAGE NOT FOUND</BODY></HTML>";
        }

        // Envío del statusLine.
        os.writeBytes(statusLine);

        // Envío del tipo de contenido.
        os.writeBytes(contentTypeLine);

        // Envío de una línea en blanco para indicar el final de header line.
        os.writeBytes(CRLF);

        // Envío del cuerpo de la página.
        if (fileExists) {
            sendBytes(fis, os); //Si la página existe se envian las líneas del archivo
                                //encontrado.
            fis.close();
        }else{
            os.writeBytes(entityBody);  //Sino se escribe las líneas del archivo no encontrad (Not found).
        }

        os.close(); //Cierre de streams y socket.
        br.close();
        socket.close();

    }

    //Need this one for sendBytes function called in processRequest
    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception
    {
        // Construcción de 1 KB de buffer para el socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copia el requested file aloutput del socket.
        while((bytes = fis.read(buffer)) != -1 ) {
           os.write(buffer, 0, bytes);
        }
    }
    
    //Esta función obtiene el tipo de archivo pedido.
    private static String contentType(String fileName)
    {
        if(fileName.endsWith(".htm") || fileName.endsWith(".html"))
            return "text/html";
        return "application/octet-stream";
    }
}