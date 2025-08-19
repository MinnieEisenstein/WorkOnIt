package com.example.workonit.net;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class HttpClients {

    // returns a normal client in release, and a trust-all client in debug
    public static OkHttpClient get(Context ctx) {
        boolean isDebug = (ctx.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (!isDebug) return new OkHttpClient();  // release: keep strict security

        try {
            final X509TrustManager trustAll = new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                @Override public void checkServerTrusted(X509Certificate[] chain, String authType) { }
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
            };
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, new TrustManager[]{trustAll}, new SecureRandom());
            SSLSocketFactory sf = ssl.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sf, trustAll)
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override public boolean verify(String hostname, SSLSession session) { return true; }
                    })
                    .build();
        } catch (Exception e) {
            return new OkHttpClient(); // fallback if anything goes wrong
        }
    }
}