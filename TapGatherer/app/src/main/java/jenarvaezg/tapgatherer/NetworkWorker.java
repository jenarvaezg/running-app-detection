package jenarvaezg.tapgatherer;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

/**
 * Created by jose on 4/13/17.
 */
public class NetworkWorker implements Runnable {

    private static final String TAG = "NetworkWorker";
    private static final String base_url = "http://jenarvaezgvps.ddns.net:8081";
    private static final String serverName = "jenarvaezgvps.ddns.net";
    private static final Integer serverPort = 8081;
    static File externalFilesDir;



    enum Urls{
        TRAIN_TAPS, TRAIN_APPS, PREDICT_TAPS, PREDICT_APPS, UPDATE_TAPS, UPDATE_APPS
    }
    private static HashMap<Urls, String> urls = initializeHashMap();
    private static String cookie;
    static HashMap<Urls, String> initializeHashMap() {
        HashMap<Urls, String> hm = new HashMap<>();
        hm.put(Urls.TRAIN_TAPS, "/train_taps");
        hm.put(Urls.TRAIN_APPS, "/train_apps");
        hm.put(Urls.PREDICT_TAPS, "/predict_taps");
        hm.put(Urls.PREDICT_APPS, "/predict_apps");
        hm.put(Urls.UPDATE_TAPS, "/update_taps");
        hm.put(Urls.UPDATE_APPS, "/update_apps");
        return hm;
    }

    private InputStreamReader isr;
    private OutputStream os;
    private Thread thread;
    private HttpURLConnection conn;
    LinkedBlockingQueue<String> toSendQueue;

    private Socket socket;

    public NetworkWorker(Urls urlEnum) throws IOException {
        String url = base_url +  urls.get(urlEnum);

        /*URL u = new URL(url);
        this.conn = (HttpURLConnection) u.openConnection();
        this.conn.setDoOutput(true);
        this.conn.setDoInput(true);
        this.conn.setRequestProperty("Content-Type", "text/csv");
        this.conn.setRequestProperty("Cookie", getCookie());
        this.conn.setRequestMethod("POST");*/
        this.toSendQueue = new LinkedBlockingQueue<>();

        socket = new Socket(serverName, serverPort);

        this.thread  = new Thread(this);
        this.thread.start();
    }

    @Override
    public void run() {
        try {
            //this.os = conn.getOutputStream();

            DataOutputStream outToServer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToServer.writeBytes("POST /predict_taps HTTP/1.1\n");
            outToServer.writeBytes("Cookie: " + getCookie() + "\n");
            outToServer.writeBytes("\n");
            outToServer.flush();
            for (String toSend = toSendQueue.take(); ; toSend = toSendQueue.take()) {
                outToServer.writeBytes(toSend);
                outToServer.flush();
            }

        } catch (InterruptedException e) {
            Log.d(TAG, Log.getStackTraceString(e));
        } catch (IOException e) {
            Log.d(TAG, Log.getStackTraceString(e));
        }
    }

    private static String getCookie(){
        if(cookie != null){
            return cookie;
        }
        try {

            File f = externalFilesDir; //assume we have external storage, fail miserably otherwise
            //open cookie file
            File cookieFile = new File(f.getAbsolutePath() + "cookie");
            if(cookieFile.exists()){
                FileInputStream fis = new FileInputStream(cookieFile);
                byte[] data = new byte[(int) cookieFile.length()];
                fis.read(data);
                fis.close();
                cookie = new String(data);
                return cookie;
            }
            FileWriter fw = new FileWriter(cookieFile);
            SecureRandom random = new SecureRandom();
            String cookieValue = new BigInteger(130, random).toString(32);
            cookie = "user=" + cookieValue;
            fw.write(cookie);
            fw.close();
            return cookie;
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return "";
        }
    }


    static String sendString(Urls urlEnum, String toSend, Boolean awaitResponse){
        String url = base_url +  urls.get(urlEnum);

        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return "";
        }
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) u.openConnection();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return "";
        }
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "text/csv");
        conn.setRequestProperty("Cookie", getCookie());

        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return "";
        }
        InputStreamReader isr;
        try {
            OutputStream os = conn.getOutputStream();
            os.write(toSend.getBytes());
            os.flush();
            Log.d(TAG,"WRITTEN");

            isr = new InputStreamReader(conn.getInputStream());
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return "";
        }

        if(!awaitResponse) return "";

        String line;

        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(isr);
            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();

    }

    public void stop(){
        Log.d(TAG, "STOPPING!!");
        this.thread.interrupt();
    }


}
