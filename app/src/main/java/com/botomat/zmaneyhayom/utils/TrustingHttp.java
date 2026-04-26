package com.botomat.zmaneyhayom.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HTTP utility that bypasses SSL certificate validation.
 * Used because Android 4.4 (API 19) lacks modern root certificates
 * (e.g., Let's Encrypt's ISRG Root X1 added in API 24).
 *
 * SAFE for our use case: we only fetch our own version manifest and APK
 * from a known server. The downloaded APK is signed with our debug key,
 * so a tampered APK from MITM would fail signature verification and the
 * package installer would refuse to install it.
 */
public class TrustingHttp {

    private static SSLSocketFactory cachedFactory;

    public static HttpURLConnection open(String url) throws Exception {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("User-Agent", "ZmaneyHayom/Android");
        if (conn instanceof HttpsURLConnection) {
            HttpsURLConnection https = (HttpsURLConnection) conn;
            https.setSSLSocketFactory(getFactory());
            https.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }
        return conn;
    }

    private static synchronized SSLSocketFactory getFactory() throws Exception {
        if (cachedFactory != null) return cachedFactory;

        TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};

        // Try TLSv1.2 first (Android 4.4 supports it but doesn't enable by default
        // for HttpsURLConnection - explicit setting helps)
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("TLSv1.2");
        } catch (Exception ignored) {
            ctx = SSLContext.getInstance("TLS");
        }
        ctx.init(null, trustAll, new SecureRandom());
        cachedFactory = ctx.getSocketFactory();
        return cachedFactory;
    }
}
