package com.perflyst.twire.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


/**
 * This is an extension of the SSLSocketFactory which enables TLS 1.2 and 1.1.
 * Created for usage on Android 4.1-4.4 devices, which haven't enabled those by default.
 * <p>
 * Copied from https://github.com/TeamNewPipe/NewPipe-legacy/blob/master/app/src/main/java
 * /org/schabi/newpipelegacy/util/TLSSocketFactoryCompat.java
 */
public class TLSSocketFactoryCompat extends SSLSocketFactory {


    private static TLSSocketFactoryCompat instance = null;

    private final SSLSocketFactory internalSSLSocketFactory;

    public TLSSocketFactoryCompat() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLSessionContext sslSessionContext = context.getServerSessionContext();
        int sessionCacheSize = sslSessionContext.getSessionCacheSize();
        if (sessionCacheSize > 0) {
            sslSessionContext.setSessionCacheSize(0);
        }
        internalSSLSocketFactory = context.getSocketFactory();
    }

    public static TLSSocketFactoryCompat getInstance()
            throws NoSuchAlgorithmException, KeyManagementException {
        if (instance != null) {
            return instance;
        }
        instance = new TLSSocketFactoryCompat();
        return instance;
    }

    public static void setAsDefault() {
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(getInstance());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
    }

    @Override
    public Socket createSocket(final Socket s, final String host, final int port,
                               final boolean autoClose) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(final String host, final int port) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localHost,
                               final int localPort) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(
                host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(final InetAddress host, final int port) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(final InetAddress address, final int port,
                               final InetAddress localAddress, final int localPort)
            throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(
                address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(final Socket socket) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
        }
        return socket;
    }
}
