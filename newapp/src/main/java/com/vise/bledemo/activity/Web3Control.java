package com.vise.bledemo.activity;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class Web3Control {
    static String Return;
    /**傳送 Delete 到Web3 將已經有的Token刪除*/
    public static void Delete_Token_onWeb3(String priv_hash){
        URL url = null;
        try {
            url = new URL("http://192.168.50.20:5000/"+priv_hash);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection httpCon = null;
        try {
            assert url != null;
            httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded" );
            httpCon.setRequestMethod("DELETE");
            System.out.println(httpCon.getResponseCode());
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(httpCon != null){
                httpCon.disconnect();
                System.out.println("Disconnect DELETE");
            }
        }
    }
    /**Post 到Web3 API*/
    public static void Post_to_Web3(final URI posturi, final StringEntity data){
        /**Post 到Web3*/
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(posturi);  // To which Webs URI
        StringEntity params = null;
        params = data;
        httppost.addHeader("Content-Type", "application/json");
        httppost.setEntity(params);
        try {
            HttpResponse httpResponse = new DefaultHttpClient().execute(httppost);
            String strResult = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
            System.out.println("String __>" + strResult);
            String[] tokens = strResult.split(": \"");
            Return = tokens[1].split("\"")[0];
            System.out.println("retrun back : "+ Return);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            httpclient.getConnectionManager().shutdown();
        }
    }
    public static String get_response(){
        return Return;
    }
}
