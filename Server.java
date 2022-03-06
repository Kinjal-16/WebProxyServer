import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
public class Server
{
    private ServerSocket sck;
    ArrayList<String> BlockedSites = new ArrayList<String>();
    HashMap<String, File> cached = new HashMap<String, File>();

    public void start(int port)
    {
        try{
            Dash t = new Dash();
            t.start();
            sck = new ServerSocket(port);
            System.out.println("Socket running at:" + sck.getLocalPort());


        while(true)
        {

            new RequestHandler(sck.accept()).start();
            
        }}catch (IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }
    
        public void stop() {
            try {
    
                sck.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    
        }
    public ArrayList<String> getBlockedSites()
    {
        return BlockedSites;
    }
    public void modifyBlockSites(ArrayList<String> temp)
    {
        BlockedSites = temp;
    }
    class Dash extends Thread {


        @Override
        public void run() {

            Terminal t = new Terminal("Dashboard");
            Server ob = new Server();
            while(true) {
                t.println("1. Blocked Websites");
                t.println("2. Block a Website");

                t.println("3. Unblock a Website");

                int a = Integer.parseInt(t.read("Enter a number"));
                if (a == 1) {
                    if (BlockedSites.isEmpty()) {
                        t.println("EmptyList");

                    } else {
                        for (String i : BlockedSites) {
                            t.println(i);
                        }
                    }

                }
                if (a == 2) {
                    t.println("Enter the URL");
                    String url = t.read("Input");
                    BlockedSites.add(url);
                    t.println("URL blocked");

                }
                if (a == 3) {
                    t.println("Enter the URL");
                    String url = t.read("Input");
                    if (BlockedSites.contains(url)) {
                        BlockedSites.remove(url);
                        t.println("URL unblocked");

                    } else {
                        t.println("URL not found");
                    }
                }
            }

        }

    }
        private class RequestHandler extends Thread {
            private Socket clientSocket;
            private BufferedWriter out;
            private BufferedReader in;


            public RequestHandler(Socket socket) {
                this.clientSocket = socket;
            }
    
            public void run() {
                try {
                    out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    System.out.println(cached.isEmpty());
                    String inputLine = in.readLine();
                    System.out.println(inputLine);
                    String request = inputLine.substring(0,inputLine.indexOf(' '));

                    String url = inputLine.substring(inputLine.indexOf(' ')+1);
                    url = url.substring(0, url.indexOf(' '));


                    for(String i:BlockedSites)
                    {
                        if(url.contains(i))
                        {
                            System.out.println("Site blocked");
                            blockURL();
                            return;
                        }
                    }


                    if(request.equals("CONNECT"))
                    {
                        String pieces[] = url.split(":");
                        if(!url.substring(0, 4).equals("http")){
                            String add = "http://";
                            url = add + url;
                        }
                        String ActualUrl = pieces[0];
                        int port  = Integer.valueOf(pieces[1]);
                        HTTPSRequest(ActualUrl,port);
                    }
                    else
                    {   if(!url.startsWith("http")){

                            String add = "http://";
                            url = add + url;                     
                        }



                        if(cached.containsKey(url)) {
                            System.out.println("Cached Website");
                            CachedRequest(url);

                        }
                        else
                            nonCachedRequest(url);
                    }
                    System.out.println(inputLine);
                    in.close();
                    out.close();
                    clientSocket.close();
    
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }


            private void HTTPSRequest(String url,int port){
                
                
        
                try{
                    
                    for(int i=0;i<5;i++){
                        in.readLine();
                    }
        
                    InetAddress add = InetAddress.getByName(url);
                    
                    Socket RemoteServerSocket = new Socket(add, port);
                    RemoteServerSocket.setSoTimeout(10000);
        
                    String line = "HTTP/1.0 200 Connection established\r\n" +
                            "Proxy-Agent: ProxyServer/1.0\r\n" +
                            "\r\n";
                    out.write(line);
                    out.flush();
                    
                    
                    
                   
        
        
                    BufferedWriter ServerOut = new BufferedWriter(new OutputStreamWriter(RemoteServerSocket.getOutputStream()));
        
                    BufferedReader ServerIn = new BufferedReader(new InputStreamReader(RemoteServerSocket.getInputStream()));
        
        
        
                    ClientToServerTunnel clientToServerHttps =
                            new ClientToServerTunnel(clientSocket.getInputStream(), RemoteServerSocket.getOutputStream());
                    
                    clientToServerHttps.start();
                    
                    
                    try {
                        byte[] buffer = new byte[4096];
                        int read;
                        do {
                            read = RemoteServerSocket.getInputStream().read(buffer);
                            if (read > 0) {
                                clientSocket.getOutputStream().write(buffer, 0, read);
                                if (RemoteServerSocket.getInputStream().available() < 1) {
                                    clientSocket.getOutputStream().flush();
                                }
                            }
                        } while (read >= 0);
                    }
                    catch (SocketTimeoutException e) {
                        
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
        
        
                    if(RemoteServerSocket != null){
                        RemoteServerSocket.close();
                    }
        
                    if(ServerIn != null){
                        ServerIn.close();
                    }
        
                    if(ServerOut != null){
                        ServerOut.close();
                    }
        
                    if(out != null){
                        out.close();
                    }
                    
                    
                } catch (SocketTimeoutException e) {
                    String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
                            "User-Agent: ProxyServer/1.0\n" +
                            "\r\n";
                    try{
                        out.write(line);
                        out.flush();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                } 
                catch (Exception e){
                    System.out.println("Invalid HTTPS : " + url );
                    e.printStackTrace();
                }
            }
            public void CachedRequest(String url) throws IOException {
                String fileExtension = url.substring(url.lastIndexOf("."));
                String fileName;
                if (url.indexOf(".") != url.lastIndexOf("."))
                    fileName = url.substring(url.indexOf(".") + 1, url.lastIndexOf("."));
                else
                    fileName = url.substring(7, url.indexOf("."));


                fileName = fileName.replace("/", "__");
                fileName = fileName.replace('.', '_');
                File cachFile = cached.get(url);
                if (fileExtension.contains("PNG") || fileExtension.contains("JPG") || fileExtension.contains("JPEG") || fileExtension.contains("GIF")) {
                    BufferedImage image = ImageIO.read(cachFile);
                    if (image == null) {
                        System.out.println(fileName + ":Image is empty");
                        String response = "HTTP/1.0 404 NOT FOUND \n" +
                                "Proxy-agent: ProxyServer/1.0\n" +
                                "\r\n";
                        out.write(response);
                        out.flush();
                    } else {
                        String response = "HTTP/1.0 200 OK\n" +
                                "Proxy-agent: ProxyServer/1.0\n" +
                                "\r\n";
                        out.write(response);
                        out.flush();
                        ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
                    }
                } else {
                    BufferedReader cachedOut = new BufferedReader(new InputStreamReader(new FileInputStream(cachFile)));
                    String response = "HTTP/1.0 200 OK\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    out.write(response);
                    out.flush();

                    String line;
                    while ((line = cachedOut.readLine()) != null) {
                        out.write(line);
                    }
                    out.flush();

                    if (cachedOut != null) {
                        cachedOut.close();
                    }


                }
            }
            public void nonCachedRequest(String url) throws IOException {
                String fileExtension = url.substring(url.lastIndexOf("."));
                String fileName;
                if(url.indexOf(".")!=url.lastIndexOf("."))
                    fileName = url.substring(url.indexOf(".")+1,url.lastIndexOf("."));
                else
                    fileName = url.substring(7,url.indexOf("."));


                fileName = fileName.replace("/", "__");
                fileName = fileName.replace('.', '_');
                if (fileExtension.contains("/")) {
                    fileExtension = fileExtension.replace("/", "__");
                    fileExtension = fileExtension.replace('.', '_');
                    fileExtension += ".html";
                }
                BufferedWriter cacheWriter = null;
                boolean caching = true;
                File cache = new File(fileName);
                try {
                    if (cache.exists() == false) {
                        cache.createNewFile();
                    }
                    cacheWriter = new BufferedWriter(new FileWriter(cache));
                }catch(IOException e)
                {
                    caching = false;
                    System.out.println("Failed to create cached file for:"+fileName);
                    e.printStackTrace();
                }
                catch (NullPointerException e) {
                    System.out.println("Null Pointer Exception for:"+fileName);
                    e.printStackTrace();
                }





                // When file is an image

                if(fileExtension.contains("PNG") || fileExtension.contains("JPG") || fileExtension.contains("JPEG")|| fileExtension.contains("GIF"))
                {
                    URL remoteUrl = new URL(url);
                    BufferedImage img = ImageIO.read(remoteUrl);
                    if(img != null)
                    {
                        ImageIO.write(img,fileExtension.substring(1),cache);
                        cached.put(fileName,cache);
                        System.out.println(cached);
                        String response = "HTTP/1.0 200 OK\n" +
                                "Proxy-agent: ProxyServer/1.0\n" +
                                "\r\n";
                        out.write(response);
                        out.flush();

                        ImageIO.write(img, fileExtension.substring(1), clientSocket.getOutputStream());
                    }
                    else {
                        System.out.println(fileName+":404 File Not Found");
                        String error = "HTTP/1.0 404 NOT FOUND\n" +
                                "Proxy-agent: ProxyServer/1.0\n" +
                                "\r\n";
                        out.write(error);
                        out.flush();
                        return;
                    }
                }

                // When File is a text file
                else {
                    URL remoteUrl = new URL(url);
                    HttpURLConnection Conn = (HttpURLConnection)remoteUrl.openConnection();
                    Conn.setRequestProperty("Content-Type",
                            "application/x-www-form-urlencoded");
                    Conn.setRequestProperty("Content-Language", "en-US");
                    Conn.setUseCaches(false);
                    Conn.setDoOutput(true);

                    // Create Buffered Reader from remote Server
                    BufferedReader ProxyIn = new BufferedReader(new InputStreamReader(Conn.getInputStream()));
                    String response = "HTTP/1.0 200 OK\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    out.write(response);
                    out.flush();
                    String read;
                    while((read = ProxyIn.readLine()) != null){
                        out.write(read);
                        if(caching==true)
                            cacheWriter.write(read);
                    }
                    out.flush();
                    if (caching) {
                        cacheWriter.flush();
                        cached.put(url, cache);
                    }
                    if(cacheWriter != null)
                    {
                        cacheWriter.close();
                    }
                    if(ProxyIn != null)
                    {
                        ProxyIn.close();
                    }
                    System.out.println(cached);


                }
            }
            public void blockURL()
            {
                try {
                    BufferedWriter bW = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    String line = "HTTP/1.0 403 Access Forbidden \n" +
                            "User-Agent: ProxyServer/1.0\n" +
                            "\r\n";
                    bW.write(line);
                    bW.flush();
                } catch (IOException e) {
                    System.out.println("Could report a blocked site to the client");
                    e.printStackTrace();
                }
            }

        }

    
        public static void main(String[] args) {


            Server server = new Server();
            server.start(5555);



        }

    
    
    
    
}
class ClientToServerTunnel extends Thread{
		
    InputStream ClientIn;
    OutputStream proxyOut;
    
  
    public ClientToServerTunnel(InputStream ClientIn, OutputStream proxyOut) {
        this.ClientIn = ClientIn;
        this.proxyOut = proxyOut;
    }

    @Override
    public void run(){
        try {
            byte[] buffer = new byte[4096];
            int read;
            do {
                read = ClientIn.read(buffer);
                if (read > 0) {
                    proxyOut.write(buffer, 0, read);
                    if (ClientIn.available() < 1) {
                        proxyOut.flush();
                    }
                }
            } while (read >= 0);
        }
        catch (SocketTimeoutException ste) {
            System.out.println("Client to Remote Server Socket Timed out");
        }
        catch (IOException e) {
            System.out.println("Proxy to client HTTPS read timed out");

        }
    }
}


