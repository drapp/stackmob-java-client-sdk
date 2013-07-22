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

package com.stackmob.sdk.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stackmob.sdk.api.*;
import com.stackmob.sdk.api.StackMob.OAuthVersion;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.net.*;
import com.stackmob.sdk.push.StackMobPushToken;
import com.stackmob.sdk.util.*;
import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The base class for StackMob's internal representation of a request. This class is only meant to be used inside the sdk
 */
public abstract class StackMobRequest {
    
    public static final List<Map.Entry<String, String>> EmptyHeaders = new ArrayList<Map.Entry<String, String>>();
    public static final List<Map.Entry<String, String>> EmptyParams = new ArrayList<Map.Entry<String, String>>();

    protected static final String SECURE_SCHEME = "https";
    protected static final String REGULAR_SCHEME = "http";
    protected static final String API_KEY_HEADER = "X-StackMob-API-Key";
    protected static final String AUTHORIZATION_HEADER = "Authorization";


    protected final ExecutorService executor;
    protected final StackMobSession session;
    protected StackMobRawCallback callback;
    protected final StackMobRedirectedCallback redirectedCallback;

    protected HttpVerb httpVerb;
    protected String methodName;

    protected String urlFormat = StackMob.DEFAULT_API_HOST;
    protected Boolean isSecure = false;
    protected List<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>();
    protected List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
    private AtomicBoolean triedRefreshToken = new AtomicBoolean(false);
    private OAuthVersion oauthVersionOverride;

    protected Gson gson;

    private OAuthService oAuthService;




    protected StackMobRequest(ExecutorService executor,
                              StackMobSession session,
                              OAuthVersion oauthVersionOverride,
                              HttpVerb verb,
                              StackMobOptions options,
                              List<Map.Entry<String, String>> params,
                              String method,
                              StackMobRawCallback cb,
                              StackMobRedirectedCallback redirCb) {
        this.executor = executor;
        this.session = session;
        this.isSecure = options.isHTTPS();
        this.httpVerb = verb;
        this.headers = options.getHeaders();
        this.params = params;
        this.methodName = method;
        this.callback = cb;
        this.redirectedCallback = redirCb;
        this.oauthVersionOverride = oauthVersionOverride;

        GsonBuilder gsonBuilder = new GsonBuilder()
                                  .registerTypeAdapter(StackMobPushToken.class, new StackMobPushToken.Deserializer())
                                  .registerTypeAdapter(StackMobPushToken.class, new StackMobPushToken.Serializer())
                                  .registerTypeAdapter(StackMobForgotPasswordEmail.class, new StackMobForgotPasswordEmail.Deserializer())
                                  .registerTypeAdapter(StackMobForgotPasswordEmail.class, new StackMobForgotPasswordEmail.Serializer())
                                  .registerTypeAdapter(StackMobNull.class, new StackMobNull.Adapter())
                                  .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.TRANSIENT, Modifier.STATIC);
        gson = gsonBuilder.create();

        if(!isOAuth2()) oAuthService = new ServiceBuilder().provider(StackMobApi.class).apiKey(session.getKey()).apiSecret(session.getSecret()).build();

    }

    public StackMobRequest setUrlFormat(String urlFmt) {
        this.urlFormat = urlFmt;
        return this;
    }

    protected abstract String getRequestBody();

    public void sendRequest() {
        try {
            if(HttpVerbWithoutPayload.GET == httpVerb) {
                sendGetRequest();
            }
            else if(HttpVerbWithoutPayload.HEAD == httpVerb) {
                sendHeadRequest();
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
                callback.unsent(ex);
            }
        }
        catch(StackMobException e) {
            callback.unsent(e);
        }
    }

    protected void sendGetRequest() throws StackMobException {
        try {
            String query = formatQueryString(this.params);
            URI uri = createURI(getScheme(), urlFormat, getPath(), query);
            OAuthRequest req = getOAuthRequest(uri.getScheme(), HttpVerbWithoutPayload.GET, uri.toString());
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

    protected void sendHeadRequest() throws StackMobException {
        try {
            String query = formatQueryString(this.params);
            URI uri = createURI(getScheme(), urlFormat, getPath(), query);
            OAuthRequest req = getOAuthRequest(uri.getScheme(), HttpVerbWithoutPayload.HEAD, uri.toString());
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
            OAuthRequest req = getOAuthRequest(uri.getScheme(), HttpVerbWithPayload.POST, uri.toString(), payload);
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
            OAuthRequest req = getOAuthRequest(uri.getScheme(), HttpVerbWithPayload.PUT, uri.toString(), payload);
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
            OAuthRequest req = getOAuthRequest(uri.getScheme(), HttpVerbWithoutPayload.DELETE, uri.toString());
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
        String domain = Http.fullDomain(scheme, host);
        StringBuilder uriBuilder = new StringBuilder().append(session.getRedirect(domain));
        if(!path.startsWith("/")) {
            uriBuilder.append("/");
        }
        uriBuilder.append(escapePath(path));

        if(query != null && query.length() > 0) {
            uriBuilder.append("?").append(query);
        }

        return new URI(uriBuilder.toString());
    }

    private String escapePath(String path) throws URISyntaxException {
        String[] parts = path.split("/");

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < parts.length; i++) {
            try {
                if(i == parts.length-1) {
                    sb.append(URLEncoder.encode(parts[i], "utf-8"));
                } else {
                    sb.append(parts[i]);
                }
            } catch(UnsupportedEncodingException e) {
                throw new URISyntaxException(parts[i], "could not be URL-encoded as UTF-8");
            }

            if(i != parts.length-1) {
                sb.append("/");
            }
        }

        return sb.toString();
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
        if(session.getHTTPSOverride() == null) {
            return isSecure ? SECURE_SCHEME : REGULAR_SCHEME;
        } else {
            return session.getHTTPSOverride() ? SECURE_SCHEME : REGULAR_SCHEME;
        }
    }

    protected static String percentEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
    }

    protected static String formatQueryString(List<Map.Entry<String, String>> params) {
        List<String> paramList = new LinkedList<String>();
        for(Map.Entry<String, String> pair : params) {
            String key = pair.getKey();
            String value = pair.getValue();
            try {
                paramList.add(String.format("%s=%s", percentEncode(key), percentEncode(value)));
            }
            catch(UnsupportedEncodingException e) {
                //do nothing
            }
        }
        return ListHelpers.join(paramList, "&");
    }


    protected String getContentType() {
        return "application/json; charset=utf-8";
    }

    protected OAuthVersion getOAuthVersion() {
        if(this.oauthVersionOverride != null) return oauthVersionOverride;
        return session.getOAuthVersion();
    }

    protected boolean isOAuth2() {
        return getOAuthVersion() == OAuthVersion.Two;
    }

    protected OAuthRequest getOAuthRequest(String scheme, HttpVerb method, String url) {
        Verb verb = Verb.valueOf(method.toString());
        OAuthRequest oReq = new OAuthRequest(verb, url);
        int apiVersion = session.getApiVersionNumber();
        final String accept = "application/vnd.stackmob+json; version="+apiVersion;

        List<Map.Entry<String, String>> headerList = new ArrayList<Map.Entry<String, String>>();

        //build basic headers
        if(!verb.equals(Verb.GET) && !verb.equals(Verb.DELETE) && !verb.equals(Verb.HEAD)) {
            headerList.add(new Pair<String, String>("Content-Type", getContentType()));
        }

        //build user headers
        boolean hasAcceptHeader = false;
        if(this.headers != null) {
            for(Map.Entry<String, String> header : this.headers) {
                if(header.getKey().equals("Accept")) hasAcceptHeader = true;
                headerList.add(new Pair<String, String>(header.getKey(), header.getValue()));
            }
        }

        if(!hasAcceptHeader) headerList.add(new Pair<String, String>("Accept", accept));
        headerList.add(new Pair<String, String>("User-Agent", session.getUserAgent()));
        String cookieHeader = session.getCookieManager().cookieHeader();
        if(cookieHeader.length() > 0) headerList.add(new Pair<String, String>("Cookie", cookieHeader));

        //add headers to request
        for(Map.Entry<String, String> header: headerList) {
            oReq.addHeader(header.getKey(), header.getValue());
        }

        switch(getOAuthVersion()) {
            case One: oAuthService.signRequest(new Token("", ""), oReq); break;
            case Two: {
                oReq.addHeader(API_KEY_HEADER, session.getKey());
                if(session.oauth2TokenValid()) {
                    String urlNoScheme = url.substring(scheme.length() + 3);
                    int firstSlash = urlNoScheme.indexOf("/");
                    String[] hostAndPort = urlNoScheme.substring(0, firstSlash).split(":");
                    String host = hostAndPort[0];
                    String port = getPort(scheme, hostAndPort);
                    String uri = urlNoScheme.substring(firstSlash);

                    oReq.addHeader(AUTHORIZATION_HEADER, session.generateMacToken(method.toString(), uri, host, port));
                }
                break;
            }
        }

        return oReq;
    }

    private String getPort(String scheme, String[] hostAndPort) {
        if(hostAndPort.length > 1) {
            return hostAndPort[1];
        } else {
            return scheme.equals(SECURE_SCHEME) ? "443" : "80";
        }
    }

    private byte[] getByteArray(InputStream is) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException ex) {
            return new byte[0];
        }

        return buffer.toByteArray();
    }

    protected OAuthRequest getOAuthRequest(String scheme, HttpVerb method, String url, String payload) {
        OAuthRequest req = getOAuthRequest(scheme, method, url);
        req.addPayload(payload);
        return req;
    }

    protected static HttpVerb getRequestVerb(OAuthRequest req) {
        HttpVerb requestVerb = HttpVerbWithoutPayload.GET;
        if(req.getVerb() == Verb.POST) requestVerb = HttpVerbWithPayload.POST;
        else if(req.getVerb() == Verb.PUT) requestVerb = HttpVerbWithPayload.PUT;
        else if(req.getVerb() == Verb.DELETE) requestVerb = HttpVerbWithoutPayload.DELETE;
        else if(req.getVerb() == Verb.HEAD) requestVerb = HttpVerbWithoutPayload.HEAD;
        return requestVerb;
    }
    
    protected static List<Map.Entry<String, String>> getRequestHeaders(OAuthRequest req) {
        List<Map.Entry<String, String>> requestHeaders = new ArrayList<Map.Entry<String, String>>();
        for(Map.Entry<String, String> header : req.getHeaders().entrySet()) {
            requestHeaders.add(header);
        }
        return requestHeaders;
    }

    protected boolean tryRefreshToken() {
        return true;
    }

    private boolean canDoRefreshToken() {
        return isOAuth2() && session.oauth2RefreshTokenValid() && tryRefreshToken() && !triedRefreshToken.get();
    }

    protected void refreshTokenAndResend() {
        triedRefreshToken.set(true);
        StackMobAccessTokenRequest.newRefreshTokenRequest(executor, session, redirectedCallback, new StackMobRawCallback() {
            @Override
            public void unsent(StackMobException e) {
               sendRequest();
            }

            @Override
            public void temporaryPasswordResetRequired(StackMobException e) {
                sendRequest();
            }

            @Override
            public void done(HttpVerb requestVerb, String requestURL, List<Map.Entry<String, String>> requestHeaders, String requestBody, Integer responseStatusCode, List<Map.Entry<String, String>> responseHeaders, byte[] responseBody) {
                sendRequest();
            }

            @Override
            public void circularRedirect(String originalUrl, Map<String, String> redirectHeaders, String redirectBody, String newURL) {

            }
        }).setUrlFormat(urlFormat).sendRequest();
    }
    
    protected void sendRequest(final OAuthRequest req) throws InterruptedException, ExecutionException {
        final StackMobRawCallback cb = this.callback;

        if(isOAuth2() && !session.oauth2TokenValid() && canDoRefreshToken()) {
            refreshTokenAndResend();
        } else {
            executor.submit(new Callable<Object>() {
                @Override
                public String call() throws Exception {
                    try {
                        session.getLogger().logInfo("%s", "Request URL: " + req.getUrl() + "\nRequest Verb: " + getRequestVerb(req) + "\nRequest Headers: " + getRequestHeaders(req) + "\nRequest Body: " + req.getBodyContents());
                        Response ret = req.send();
                        byte[] rawBody;
                        String stringBody;
                        try {
                           // Apparently sometime this just NPEs
                           rawBody = getByteArray(ret.getStream());
                           stringBody = new String(rawBody, "UTF-8");
                        } catch(Exception e) {
                           stringBody = "{}";
                           rawBody = new byte[0];
                        }
                        String trimmedBody = stringBody.length() < 1000 ? stringBody : (stringBody.subSequence(0, 1000) + " (truncated)");
                        session.getLogger().logInfo("%s", "Response StatusCode: " + ret.getCode() + "\nResponse Headers: " + ret.getHeaders() + "\nResponse: " + trimmedBody);
                        if(!isOAuth2() && ret.getHeaders() != null) session.recordServerTimeDiff(ret.getHeader("Date"));
                        if(HttpRedirectHelper.isRedirected(ret.getCode())) {
                            session.getLogger().logInfo("Response was redirected");
                            String newLocation = HttpRedirectHelper.getNewLocation(ret.getHeaders());
                            URL url = new URL(newLocation);
                            String oldDomain = Http.fullDomain(getScheme(), urlFormat);
                            String newDomain = Http.fullDomain(url.getProtocol(), url.getAuthority());
                            if(session.getRedirect(oldDomain).equals(newDomain)) {
                                callback.circularRedirect(req.getUrl(), ret.getHeaders(), stringBody, newLocation);
                            } else {
                                session.setRedirect(oldDomain, newDomain, HttpRedirectHelper.isPermanentRedirect(ret.getCode()));
                                HttpVerb verb = HttpVerbHelper.valueOf(req.getVerb().toString());
                                OAuthRequest newReq = getOAuthRequest(url.getProtocol(), verb, newLocation);
                                if(req.getBodyContents() != null && req.getBodyContents().length() > 0) {
                                    newReq = getOAuthRequest(url.getProtocol(), verb, newLocation, req.getBodyContents());
                                }
                                redirectedCallback.redirected(req.getUrl(), ret.getHeaders(), stringBody, newReq.getUrl());
                                if(callback.redirected(req.getUrl(), ret.getHeaders(), stringBody, newReq.getUrl())) {
                                    sendRequest(newReq);
                                }
                            }
                        }
                        else {
                            List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
                            if(ret.getHeaders() != null) {
                                for(Map.Entry<String, String> header : ret.getHeaders().entrySet()) {
                                    headers.add(header);
                                }
                            }
                            if(Http.isSuccess(ret.getCode())) {
                                session.getCookieManager().storeCookies(ret);
                            }
                            boolean retried = false;
                            if(Http.isUnavailable(ret.getCode())) {
                                int afterMilliseconds = -1;
                                for(Map.Entry<String, String> headerPair : headers) {
                                    if(Http.isRetryAfterHeader(headerPair.getKey())) {
                                        try {
                                            int candidateMilliseconds = Integer.parseInt(headerPair.getValue()) * 1000;
                                            if(candidateMilliseconds > 0) {
                                                afterMilliseconds = candidateMilliseconds;
                                            }
                                        } catch(Throwable ignore) { }
                                    }
                                }
                                if(afterMilliseconds != -1 && cb.getRetriesRemaining() > 0 && cb.retry(afterMilliseconds)) {
                                    cb.setRetriesRemaining(cb.getRetriesRemaining() - 1);
                                    sendRequest();
                                    retried = true;
                                }
                            }
                            if(!retried) {
                                if(ret.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED && canDoRefreshToken()) {
                                    refreshTokenAndResend();
                                } else {
                                    try {
                                        cb.setDone(getRequestVerb(req),
                                                req.getUrl(),
                                                getRequestHeaders(req),
                                                req.getBodyContents(),
                                                ret.getCode(),
                                                headers,
                                                rawBody);
                                    }
                                    catch(Throwable t) {
                                        session.getLogger().logError("Callback threw error %s", StackMobLogger.getStackTrace(t));
                                    }
                                }
                            }
                        }
                    } catch(OAuthException e) {
                        session.getLogger().logWarning("Unexpected OAuth exception prevented message from being sent %s", StackMobLogger.getStackTrace(e));
                        cb.unsent(new StackMobException(e.getMessage()));

                    } catch(Throwable t) {
                        session.getLogger().logWarning("Invoking callback after unexpected exception %s", StackMobLogger.getStackTrace(t));
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

}
