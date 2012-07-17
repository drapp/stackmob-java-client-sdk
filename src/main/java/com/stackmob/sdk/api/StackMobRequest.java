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

package com.stackmob.sdk.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stackmob.sdk.api.StackMob.OAuthVersion;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.net.*;
import com.stackmob.sdk.push.StackMobPushToken;
import com.stackmob.sdk.push.StackMobPushTokenDeserializer;
import com.stackmob.sdk.push.StackMobPushTokenSerializer;
import com.stackmob.sdk.util.Http;
import com.stackmob.sdk.util.Pair;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public abstract class StackMobRequest {
    
    public static final List<Map.Entry<String, String>> EmptyHeaders = new ArrayList<Map.Entry<String, String>>();
    public static final Map<String, String> EmptyParams = new HashMap<String, String>();

    public static final String DEFAULT_URL_FORMAT = "mob1.stackmob.com";
    public static final String DEFAULT_API_URL_FORMAT = "api." + DEFAULT_URL_FORMAT;
    public static final String DEFAULT_PUSH_URL_FORMAT = "push." + DEFAULT_URL_FORMAT;
    protected static final String SECURE_SCHEME = "https";
    protected static final String REGULAR_SCHEME = "http";
    protected static final String API_KEY_HEADER = "X-StackMob-API-Key";
    protected static final String AUTHORIZATION_HEADER = "Authorization";
    private static StackMobCookieStore cookieStore = new StackMobCookieStore();

    public static void setCookieStore(StackMobCookieStore store) {
        cookieStore = store;
    }

    public static StackMobCookieStore getCookieStore() {
        return cookieStore;
    }

    protected final ExecutorService executor;
    protected final StackMobSession session;
    protected StackMobRawCallback callback;
    protected final StackMobRedirectedCallback redirectedCallback;

    protected HttpVerb httpVerb;
    protected String methodName;

    protected String urlFormat = DEFAULT_API_URL_FORMAT;
    protected Boolean isSecure = false;
    protected Map<String, String> params = new HashMap<String, String>();
    protected List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();

    protected Gson gson;

    private OAuthService oAuthService;

    protected StackMobRequest(ExecutorService executor,
                              StackMobSession session,
                              HttpVerb verb,
                              List<Map.Entry<String, String>> headers,
                              Map<String, String> params,
                              String method,
                              StackMobRawCallback cb,
                              StackMobRedirectedCallback redirCb) {
        this.executor = executor;
        this.session = session;
        this.httpVerb = verb;
        this.headers = headers;
        this.params = params;
        this.methodName = method;
        this.callback = cb;
        this.redirectedCallback = redirCb;

        GsonBuilder gsonBuilder = new GsonBuilder()
                                  .registerTypeAdapter(StackMobPushToken.class, new StackMobPushTokenDeserializer())
                                  .registerTypeAdapter(StackMobPushToken.class, new StackMobPushTokenSerializer())
                                  .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.TRANSIENT, Modifier.STATIC);
        gson = gsonBuilder.create();

        oAuthService = new ServiceBuilder().provider(StackMobApi.class).apiKey(session.getKey()).apiSecret(session.getSecret()).build();

    }

    public StackMobRequest setUrlFormat(String urlFmt) {
        this.urlFormat = urlFmt;
        return this;
    }

    protected abstract String getRequestBody();

    public StackMobRequestSendResult sendRequest() {
        try {
            if(HttpVerbWithoutPayload.GET == httpVerb) {
                sendGetRequest();
            }
            else if(HttpVerbWithPayload.POST == httpVerb) {
                sendPostRequest();
            }
            else if(HttpVerbWithPayload.PUT == httpVerb) {
                sendPutRequest();
            }
            else if(HttpVerbWithoutPayload.DELETE == httpVerb) {
                sendDeleteRequest();
            }
            else {
                StackMobException ex = new StackMobException(String.format("The StackMob SDK doesn't support the HTTP verb %s at this time", httpVerb.toString()));
                return new StackMobRequestSendResult(StackMobRequestSendResult.RequestSendStatus.FAILED, ex);
            }
            return new StackMobRequestSendResult();
        }
        catch(StackMobException e) {
            return new StackMobRequestSendResult(StackMobRequestSendResult.RequestSendStatus.FAILED, e);
        }
    }

    protected void sendGetRequest() throws StackMobException {
        try {
            String query = formatQueryString(this.params);
            URI uri = createURI(getScheme(), urlFormat, getPath(), query);
            OAuthRequest req = getOAuthRequest(HttpVerbWithoutPayload.GET, uri.toString());
            sendRequest(req);
        }
        catch (URISyntaxException e) {
            throw new StackMobException(e.getMessage());
        }
        catch (InterruptedException e) {
            throw new StackMobException(e.getMessage());
        }
        catch (ExecutionException e) {
            throw new StackMobException(e.getMessage());
        }
    }

    protected void sendPostRequest() throws StackMobException {
        try {
            URI uri = createURI(getScheme(), urlFormat, getPath(), "");
            String payload = getRequestBody();
            OAuthRequest req = getOAuthRequest(HttpVerbWithPayload.POST, uri.toString(), payload);
            sendRequest(req);
        }
        catch (URISyntaxException e) {
            throw new StackMobException(e.getMessage());
        }
        catch (InterruptedException e) {
            throw new StackMobException(e.getMessage());
        }
        catch (ExecutionException e) {
            throw new StackMobException(e.getMessage());
        }
    }

    protected void sendPutRequest() throws StackMobException {
        try {
            URI uri = createURI(getScheme(), urlFormat, getPath(), "");
            String payload = getRequestBody();
            OAuthRequest req = getOAuthRequest(HttpVerbWithPayload.PUT, uri.toString(), payload);
            sendRequest(req);
        }
        catch (URISyntaxException e) {
            throw new StackMobException(e.getMessage());
        }
        catch (InterruptedException e) {
            throw new StackMobException(e.getMessage());
        }
        catch (ExecutionException e) {
            throw new StackMobException(e.getMessage());
        }
    }

    protected void sendDeleteRequest() throws StackMobException {
        try {
            String query = formatQueryString(this.params);
            URI uri = createURI(getScheme(), urlFormat, getPath(), query);
            OAuthRequest req = getOAuthRequest(HttpVerbWithoutPayload.DELETE, uri.toString());
            sendRequest(req);
        }
        catch (URISyntaxException e) {
            throw new StackMobException(e.getMessage());
        }
        catch (InterruptedException e) {
            throw new StackMobException(e.getMessage());
        }
        catch (ExecutionException e) {
            throw new StackMobException(e.getMessage());
        }
    }

    protected URI createURI(String scheme, String host, String path, String query) throws URISyntaxException {
        StringBuilder uriBuilder = new StringBuilder().append(scheme).append("://").append(host);
        if(!path.startsWith("/")) {
            uriBuilder.append("/");
        }
        uriBuilder.append(path);

        if(query != null && query.length() > 0) {
            uriBuilder.append("?").append(query);
        }

        return new URI(uriBuilder.toString());
    }

    protected String getPath() {
        if(methodName.startsWith("/")) {
            return methodName;
        }
        else {
            return "/" + methodName;
        }
    }

    protected String getScheme() {
        if (isSecure && (session.getHTTPSOverride() == null || session.getHTTPSOverride() )) {
            return SECURE_SCHEME;
        }
        else {
            return REGULAR_SCHEME;
        }
    }

    protected static String percentEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
    }

    protected static String formatQueryString(Map<String, String> params) {
        StringBuilder formatBuilder = new StringBuilder();
        boolean first = true;
        for(String key : params.keySet()) {
            if(!first) {
                formatBuilder.append("&");
            }
            first = false;
            String value = params.get(key);
            try {
                formatBuilder.append(percentEncode(key)).append("=").append(percentEncode(value));
            }
            catch(UnsupportedEncodingException e) {
                //do nothing
            }
        }
        return formatBuilder.toString();
    }


    protected String getContentType() {
        return "application/json";
    }


    protected OAuthRequest getOAuthRequest(HttpVerb method, String url) {
        Verb verb = Verb.valueOf(method.toString());
        OAuthRequest oReq = new OAuthRequest(verb, url);
        int apiVersion = session.getApiVersionNumber();
        final String accept = "application/vnd.stackmob+json; version="+apiVersion;

        List<Map.Entry<String, String>> headerList = new ArrayList<Map.Entry<String, String>>();

        //build basic headers
        if(!verb.equals(Verb.GET) && !verb.equals(Verb.DELETE)) {
            headerList.add(new Pair<String, String>("Content-Type", getContentType()));
        }
        headerList.add(new Pair<String, String>("Accept", accept));
        headerList.add(new Pair<String, String>("User-Agent", StackMob.getUserAgent(session.getAppName())));
        String cookieHeader = cookieStore.cookieHeader();
        if(cookieHeader.length() > 0) headerList.add(new Pair<String, String>("Cookie", cookieHeader));

        //build user headers
        if(this.headers != null) {
            for(Map.Entry<String, String> header : this.headers) {
                headerList.add(new Pair<String, String>(header.getKey(), header.getValue()));
            }
        }

        //add headers to request
        for(Map.Entry<String, String> header: headerList) {
            oReq.addHeader(header.getKey(), header.getValue());
        }

        switch(session.getOAuthVersion()) {
            case One: oAuthService.signRequest(new Token("", ""), oReq); break;
            case Two: {
                oReq.addHeader(API_KEY_HEADER, session.getKey());
                if(session.oauth2TokenValid()) {
                    String urlNoScheme = url.substring(getScheme().length() + 3);
                    int firstSlash = urlNoScheme.indexOf("/");
                    String[] hostAndPort = urlNoScheme.substring(0, firstSlash).split(":");
                    String host = hostAndPort[0];
                    String port = hostAndPort.length > 1 ? hostAndPort[1] : "80";
                    String uri = urlNoScheme.substring(firstSlash);

                    oReq.addHeader(AUTHORIZATION_HEADER, session.generateMacToken(method.toString(), uri, host, port));
                }
                break;
            }
        }

        return oReq;
    }

    protected OAuthRequest getOAuthRequest(HttpVerb method, String url, String payload) {
        OAuthRequest req = getOAuthRequest(method, url);
        req.addPayload(payload);
        return req;
    }

    protected static HttpVerb getRequestVerb(OAuthRequest req) {
        HttpVerb requestVerb = HttpVerbWithoutPayload.GET;
        if(req.getVerb() == Verb.POST) requestVerb = HttpVerbWithPayload.POST;
        else if(req.getVerb() == Verb.PUT) requestVerb = HttpVerbWithPayload.PUT;
        else if(req.getVerb() == Verb.DELETE) requestVerb = HttpVerbWithoutPayload.DELETE;
        return requestVerb;
    }
    
    protected static List<Map.Entry<String, String>> getRequestHeaders(OAuthRequest req) {
        List<Map.Entry<String, String>> requestHeaders = new ArrayList<Map.Entry<String, String>>();
        for(Map.Entry<String, String> header : req.getHeaders().entrySet()) {
            requestHeaders.add(header);
        }
        return requestHeaders;
    }
    
    protected void sendRequest(final OAuthRequest req) throws InterruptedException, ExecutionException {
        final StackMobRawCallback cb = this.callback;


        executor.submit(new Callable<Object>() {
            @Override
            public String call() throws Exception {
                try {
                    StackMob.getLogger().logInfo("%s", "Request URL: " + req.getUrl() + "\nRequest Verb: " + getRequestVerb(req) + "\nRequest Headers: " + getRequestHeaders(req) + "\nRequest Body: " + req.getBodyContents());
                    Response ret = req.send();
                    StackMob.getLogger().logInfo("%s", "Response StatusCode: " + ret.getCode() + "\nResponse Headers: " + ret.getHeaders() + "\nResponse: " + (ret.getBody().length() < 1000 ? ret.getBody() : (ret.getBody().subSequence(0, 1000) + " (truncated)")));
                    if(!session.isOAuth2() && ret.getHeaders() != null) session.recordServerTimeDiff(ret.getHeader("Date"));
                    if(HttpRedirectHelper.isRedirected(ret.getCode())) {
                        StackMob.getLogger().logInfo("Response was redirected");
                        String newLocation = HttpRedirectHelper.getNewLocation(ret.getHeaders());
                        HttpVerb verb = HttpVerbHelper.valueOf(req.getVerb().toString());
                        OAuthRequest newReq = getOAuthRequest(verb, newLocation);
                        if(req.getBodyContents() != null && req.getBodyContents().length() > 0) {
                            newReq = getOAuthRequest(verb, newLocation, req.getBodyContents());
                        }
                        //does NOT protect against circular redirects
                        redirectedCallback.redirected(req.getUrl(), ret.getHeaders(), ret.getBody(), newReq.getUrl());
                        sendRequest(newReq);
                    }
                    else {
                        List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
                        if(ret.getHeaders() != null) {
                            for(Map.Entry<String, String> header : ret.getHeaders().entrySet()) {
                                headers.add(header);
                            }
                        }
                        if(Http.isSuccess(ret.getCode())) {
                            cookieStore.storeCookies(ret);
                        }
                        try {
                            cb.setDone(getRequestVerb(req),
                                    req.getUrl(),
                                    getRequestHeaders(req),
                                    req.getBodyContents(),
                                    ret.getCode(),
                                    headers,
                                    ret.getBody().getBytes());
                        }
                        catch(Throwable t) {
                            StackMob.getLogger().logError("Callback threw error %s", StackMobLogger.getStackTrace(t));
                        }
                    }
                }
                catch(Throwable t) {
                    StackMob.getLogger().logWarning("Invoking callback after unexpected exception %s", StackMobLogger.getStackTrace(t));
                    cb.setDone(getRequestVerb(req),
                               req.getUrl(),
                               getRequestHeaders(req),
                               req.getBodyContents(),
                               -1,
                               EmptyHeaders,
                               t.getMessage().getBytes());
                }
                return null;
            }
        });
    }

}