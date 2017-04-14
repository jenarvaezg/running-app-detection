package jenarvaezg.tapgatherer;

import android.util.Log;

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
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

/**
 * Created by jose on 4/13/17.
 */
public class NetworkWorker   {

    private static final String TAG = "NetworkWorker";
    private static final String base_url = "http://jenarvaezgvps.ddns.net:8081";
    static File externalFilesDir;

    static enum Urls{
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

    static String getCookie(){
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

        URL u = null;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return "";
        }
        Log.d(TAG, "URL: " + url.toString());
        HttpURLConnection conn = null;
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
        InputStreamReader isr = null;
        Log.d(TAG, Integer.toString(toSend.split("\n").length));
        try {
            OutputStream os = conn.getOutputStream();
            os.write(toSend.getBytes());
            os.flush();
            /*gzos = new GZIPOutputStream(conn.getOutputStream());
            gzos.write(toSend.getBytes());
            gzos.flush();*/
            Log.d(TAG,"WRITTEN");

            isr = new InputStreamReader(conn.getInputStream());
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return "";
        }

        if(!awaitResponse){
            return "";
        }

        String line = "";

        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(isr);
            while ((line = reader.readLine()) != null)
            {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();

    }



}
