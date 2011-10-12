/**
 * Copyright 2011 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.sdk.net;

import java.net.URI;

import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import com.stackmob.sdk.exception.StackMobException;

public class HttpHelper {
    private static final int CONN_TIMEOUT = 20000;
    private static final String DEFAULT_CONTENT_TYPE_FMT = "application/vnd.stackmob+json; version=%d";
    private static String DEFAULT_CONTENT_TYPE;
    private static DefaultHttpClient mHttpClient;
    private static OAuthConsumer mConsumer;

    //GET
    public static String doGet(URI uri, String sessionKey, String sessionSecret, Integer apiVersionNum, StackMobRedirectedCallback cb) throws StackMobException {
        return doRequest(setHeaders(new HttpGet(uri), apiVersionNum), sessionKey, sessionSecret, cb);
    }

    public static String doGet(URI uri, String sessionKey, String sessionSecret, String appName, Integer apiVersionNum, StackMobRedirectedCallback cb) throws StackMobException {
        return doRequest(setHeaders(new HttpGet(uri), appName, apiVersionNum), sessionKey, sessionSecret, cb);
    }

    //POST
    public static String doPost(URI uri, HttpEntity entity, String sessionKey, String sessionSecret, Integer apiVersionNum, StackMobRedirectedCallback cb) throws StackMobException {
        return doRequest(setHeaders(new HttpPost(uri), apiVersionNum, entity), sessionKey, sessionSecret, cb);
    }

    public static String doPost(URI uri, HttpEntity entity, String sessionKey, String sessionSecret, String appName, Integer apiVersionNum, StackMobRedirectedCallback cb)
        throws StackMobException {
        return doRequest(setHeaders(new HttpPost(uri), appName, apiVersionNum, entity), sessionKey, sessionSecret, cb);
    }

    //PUT
    public static String doPut(URI uri, HttpEntity entity, String sessionKey, String sessionSecret, Integer apiVersionNum, StackMobRedirectedCallback cb) throws StackMobException {
        return doRequest(setHeaders(new HttpPut(uri), apiVersionNum), sessionKey, sessionSecret, cb);
    }

    public static String doPut(URI uri, HttpEntity entity, String sessionKey, String sessionSecret, String appName, Integer apiVersionNum, StackMobRedirectedCallback cb)
        throws StackMobException {
        return doRequest(setHeaders(new HttpPut(uri), appName, apiVersionNum), sessionKey, sessionSecret, cb);
    }

    //DELETE
    public static String doDelete(URI uri, String sessionKey, String sessionSecret, Integer apiVersionNum, StackMobRedirectedCallback cb) throws StackMobException {
        return doRequest(setHeaders(new HttpDelete(uri), apiVersionNum), sessionKey, sessionSecret, cb);
    }

    public static String doDelete(URI uri, String sessionKey, String sessionSecret, String appName, Integer apiVersionNum, StackMobRedirectedCallback cb) throws StackMobException {
        return doRequest(setHeaders(new HttpDelete(uri), appName, apiVersionNum), sessionKey, sessionSecret, cb);
    }

    private static DefaultHttpClient setupHttpClient(String sessionKey, String sessionSecret, StackMobRedirectedCallback redirCB) {
        HttpParams httpParams = new BasicHttpParams();
        setConnectionParams(httpParams);
        SchemeRegistry schemeRegistry = registerFactories();
        ClientConnectionManager clientConnectionManager = new ThreadSafeClientConnManager(schemeRegistry);

        DefaultHttpClient client = new DefaultHttpClient(clientConnectionManager, httpParams);
        client.setRedirectStrategy(new HttpRedirectStrategy(redirCB));

        mConsumer = new CommonsHttpOAuthConsumer(sessionKey, sessionSecret);

        return client;
    }

    public static void setVersion(int version) {
        DEFAULT_CONTENT_TYPE = String.format(DEFAULT_CONTENT_TYPE_FMT, version);
    }

    private static synchronized void ensureHttpClient(String sessionKey, String sessionSecret, StackMobRedirectedCallback redirCB) {
        if (mHttpClient == null) {
            mHttpClient = setupHttpClient(sessionKey, sessionSecret, redirCB);
        }
    }

    //private helpers

    private static <T extends HttpRequestBase> T setHeaders(T req, String userAgent) {
        req.setHeader(HttpHeaders.CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        req.setHeader(HttpHeaders.ACCEPT, DEFAULT_CONTENT_TYPE);
        req.setHeader(HttpHeaders.USER_AGENT, userAgent);
        return req;
    }

    private static <T extends HttpRequestBase> T setHeaders(T req, Integer apiVersionNum) {
        return setHeaders(req, "Stackmob Android; " + apiVersionNum);
    }

    private static <T extends HttpEntityEnclosingRequestBase> T setHeaders(T req, String appName, Integer apiVersionNum, HttpEntity entity) {
        T request = setHeaders(req, appName, apiVersionNum);
        if(entity != null) {
            request.setEntity(entity);
        }
        return req;
    }

    private static <T extends HttpEntityEnclosingRequestBase> T setHeaders(T req, Integer apiVersionNum, HttpEntity entity) {
        T request = setHeaders(req, apiVersionNum);
        if(entity != null) {
            request.setEntity(entity);
        }
        return req;
    }

    private static <T extends HttpRequestBase> T setHeaders(T req, String appName, Integer apiVersionNum) {
        return setHeaders(req, "Stackmob Android; " + apiVersionNum + "/" + appName);
    }

    private static String doRequest(HttpRequestBase req, String sessionKey, String sessionSecret, StackMobRedirectedCallback cb) throws StackMobException {
        ensureHttpClient(sessionKey, sessionSecret, cb);
        try {
            mConsumer.sign(req);
            return mHttpClient.execute(req, new BasicResponseHandler());
        }
        catch (Throwable e) {
            throw new StackMobException(e.getMessage());
        }
    }

    private static SchemeRegistry registerFactories() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
        return schemeRegistry;
    }

    private static void setConnectionParams(HttpParams httpParams) {
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
        HttpConnectionParams.setConnectionTimeout(httpParams, CONN_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, CONN_TIMEOUT);
    }
}