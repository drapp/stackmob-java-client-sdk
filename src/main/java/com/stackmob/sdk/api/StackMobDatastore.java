/**
 * Copyright 2012 StackMob
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

import com.google.gson.*;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.net.HttpVerb;
import com.stackmob.sdk.net.HttpVerbWithPayload;
import com.stackmob.sdk.net.HttpVerbWithoutPayload;
import com.stackmob.sdk.request.StackMobRequest;
import com.stackmob.sdk.request.StackMobRequestWithPayload;
import com.stackmob.sdk.request.StackMobRequestWithoutPayload;
import com.stackmob.sdk.util.Http;
import com.stackmob.sdk.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class StackMobDatastore {


    private ExecutorService executor;
    private StackMobSession session;
    private String host;
    private StackMobRedirectedCallback redirectedCallback;

    public StackMobDatastore(ExecutorService executor, StackMobSession session, String host, StackMobRedirectedCallback redirectedCallback) {
        this.executor = executor;
        this.session = session;
        this.host = host;
        this.redirectedCallback = redirectedCallback;
    }

    /**
     * do a get request on the StackMob platform
     * @param path the path to get
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void get(String path, StackMobRawCallback callback) {
        new StackMobRequestWithoutPayload(this.executor,
                                          this.session,
                                          HttpVerbWithoutPayload.GET,
                                          StackMobRequest.EmptyHeaders,
                                          StackMobRequest.EmptyParams,
                                          path,
                                          callback,
                                          this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * do a get request on the StackMob platform
     * @param path the path to get
     * @param options additional options, such as headers, to modify the request
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void get(String path, StackMobOptions options, StackMobRawCallback callback) {
        get(path, StackMobRequest.EmptyParams, options.getHeaders(), callback);
    }

    /**
     * do a get request on the StackMob platform
     * @param path the path to get
     * @param arguments arguments to be encoded into the query string of the get request
     * @param headers any additional headers to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    private void get(String path, Map<String, String> arguments, List<Map.Entry<String, String>> headers, StackMobRawCallback callback) {
        new StackMobRequestWithoutPayload(this.executor,
                                          this.session,
                                          HttpVerbWithoutPayload.GET,
                                          headers,
                                          arguments,
                                          path,
                                          callback,
                                          this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * do a get request on the StackMob platform
     * @param query the query to run
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void get(StackMobQuery query, StackMobRawCallback callback) {
        StackMobOptions options = new StackMobOptions().headers(query.getHeaders());
        this.get("/"+query.getObjectName(), query.getArguments(), options.getHeaders(), callback);
    }

    /**
     * do a get request on the StackMob platform
     * @param query the query to run
     * @param options additional options, such as headers, to modify the request
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void get(StackMobQuery query, StackMobOptions options, StackMobRawCallback callback) {
        this.get("/"+query.getObjectName(), query.getArguments(), options.headers(query.getHeaders()).getHeaders(), callback);
    }


    /**
     * do a post request on the StackMob platform for a single object
     * @param path the path to get
     * @param requestObject the object to serialize and send in the POST body. this object will be serialized with Gson
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void post(String path, Object requestObject, StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.POST,
                                       StackMobRequest.EmptyHeaders,
                                       StackMobRequest.EmptyParams,
                                       requestObject,
                                       path,
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * do a post request on the StackMob platform for a single object
     * @param path the path to get
     * @param requestObject the object to serialize and send in the POST body. this object will be serialized with Gson
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void post(String path, Object requestObject, StackMobOptions options, StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.POST,
                                       options.getHeaders(),
                                       StackMobRequest.EmptyParams,
                                       requestObject,
                                       path,
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * do a post request on the StackMob platform for a single object
     * @param path the path to get
     * @param body the json body
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void post(String path, String body, StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.POST,
                                       StackMobRequest.EmptyHeaders,
                                       StackMobRequest.EmptyParams,
                                       body,
                                       path,
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * do a POST request on the StackMob platform for a single object
     * @param path the path to get
     * @param body the json body
     * @param options any additional headers to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void post(String path, String body, StackMobOptions options, StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.POST,
                                       options.getHeaders(),
                                       StackMobRequest.EmptyParams,
                                       body,
                                       path,
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * do a post request on the StackMob platform with a list of objects
     * @param path the path to get
     * @param requestObjects List of objects to serialize and send in the POST body. the list will be serialized with Gson
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public <T> void postBulk(String path, List<T> requestObjects, StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.POST,
                                       StackMobRequest.EmptyHeaders,
                                       StackMobRequest.EmptyParams,
                                       requestObjects,
                                       path,
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * post a new related object to an existing object. the relation of the root object is updated
     * @param path the path to get
     * @param primaryId id of the object with the relation
     * @param relatedField name of the relation
     * @param relatedObject related object to post
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void postRelated(String path, String primaryId, String relatedField, Object relatedObject, StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.POST,
                                       StackMobRequest.EmptyHeaders,
                                       StackMobRequest.EmptyParams,
                                       relatedObject,
                                       String.format("%s/%s/%s", path, primaryId, relatedField),
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * post a new related object to an existing object. the relation of the root object is updated
     * @param path the path to get
     * @param primaryId id of the object with the relation
     * @param relatedField name of the relation
     * @param relatedObject related object to post
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void postRelated(String path, String primaryId, String relatedField, String relatedObject, StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.POST,
                                       StackMobRequest.EmptyHeaders,
                                       StackMobRequest.EmptyParams,
                                       relatedObject,
                                       String.format("%s/%s/%s", path, primaryId, relatedField),
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * post a list of new related objects to an existing object. the relation of the root object is updated
     * @param path the path to get
     * @param primaryId id of the object with the relation
     * @param relatedField name of the relation
     * @param relatedObjects list of related objects to post. the list will be serialized with Gson
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public <T> void postRelatedBulk(String path, String primaryId, String relatedField, List<T> relatedObjects, StackMobRawCallback callback) {
        postRelated(path, primaryId, relatedField, relatedObjects, callback);
    }


    /**
     * do a PUT request on the StackMob platform
     * @param path the path to PUT
     * @param id the id of the object to PUT
     * @param requestObject the object to serialize and send in the PUT body. this object will be serialized with Gson
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void put(String path, String id, Object requestObject, StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.PUT,
                                       StackMobRequest.EmptyHeaders,
                                       StackMobRequest.EmptyParams,
                                       requestObject,
                                       path + "/" + id,
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * do a put request on the StackMob platform
     * @param path the path to put
     * @param id the id of the object to put
     * @param body the json body
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void put(String path, String id, String body, StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.PUT,
                                       StackMobRequest.EmptyHeaders,
                                       StackMobRequest.EmptyParams,
                                       body,
                                       path + "/" + id,
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * do a PUT request on the StackMob platform, treating some of the fields as counters to be incremented rather
     * than as values to set
     * @param path the path to put
     * @param id the id of the object to put
     * @param requestObject the object to serialize and send in the PUT body. this object will be serialized with Gson
     * @param counterFields a list of the fields in the object to be treated as counters being incremented
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void putAndUpdateAtomicCounters(String path,
                                           String id,
                                           Object requestObject,
                                           List<String> counterFields,
                                           StackMobRawCallback callback) {
        JsonObject obj = new Gson().toJsonTree(requestObject).getAsJsonObject();
        for(Map.Entry<String, JsonElement> field : new HashSet<Map.Entry<String, JsonElement>>(obj.entrySet())) {
            if(counterFields.contains(field.getKey())) {
                obj.remove(field.getKey());
                obj.add(field.getKey() + "[inc]", field.getValue());
            }
        }
        put(path, id, obj.toString(), callback);
    }

    /**
     * do an atomic update on a an integer field in a particular object and schema
     * @param path the path to put
     * @param id the id of the object to put
     * @param field the field to increment
     * @param value the value to increment by
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void updateAtomicCounter(String path,
                                    String id,
                                    String field,
                                    int value,
                                    StackMobRawCallback callback) {
        JsonObject body = new JsonObject();
        body.add(field + "[inc]", new JsonPrimitive(value));
        put(path, id, body.toString(), callback);
    }

    /**
     * do a an atomic put request on the StackMob platform with the contents of the has-many relation
     * @param path the path to get
     * @param primaryId id of the object with the relation
     * @param relatedField name of the relation
     * @param relatedIds list of ids to atomically add to the relation. The type should be the same type as the primary
     *                   key field of the related object
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public <T> void putRelated(String path,
                               String primaryId,
                               String relatedField,
                               List<T> relatedIds,
                               StackMobRawCallback callback) {
        new StackMobRequestWithPayload(this.executor,
                                       this.session,
                                       HttpVerbWithPayload.PUT,
                                       StackMobRequest.EmptyHeaders,
                                       StackMobRequest.EmptyParams,
                                       relatedIds,
                                       String.format("%s/%s/%s", path, primaryId, relatedField),
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }


    /**
     * do a DELETE request to the StackMob platform
     * @param path the path to delete
     * @param id the id of the object to put
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void delete(String path, String id, StackMobRawCallback callback) {
        new StackMobRequestWithoutPayload(this.executor,
                                          this.session,
                                          HttpVerbWithoutPayload.DELETE,
                                          StackMobRequest.EmptyHeaders,
                                          StackMobRequest.EmptyParams,
                                          path + "/" + id,
                                          callback,
                                          this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * atomically remove elements from an array or has many relationship
     * @param path the path to get
     * @param primaryId id of the object with the relation
     * @param field name of the relation or array field to delete from
     * @param idsToDelete list of ids to atomically remove from field.
     *                    ids should be same type as the primary id of the related type (most likely String or Integer)
     * @param cascadeDeletes true if related objects specified in idsToDelete should also be deleted
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public <T> void deleteIdsFrom(String path,
                                  String primaryId,
                                  String field,
                                  List<T> idsToDelete,
                                  boolean cascadeDeletes,
                                  StackMobRawCallback callback) {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < idsToDelete.size(); i++) {
            ids.append(idsToDelete.get(i).toString());
            if (i < idsToDelete.size() - 1) {
                ids.append(",");
            }
        }
        List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
        if (cascadeDeletes) {
            headers.add(new Pair<String, String>("X-StackMob-CascadeDelete", "true"));
        }
        new StackMobRequestWithoutPayload(this.executor,
                                          this.session,
                                          HttpVerbWithoutPayload.DELETE,
                                          headers,
                                          StackMobRequest.EmptyParams,
                                          String.format("%s/%s/%s/%s", path, primaryId, field, ids.toString()),
                                          callback,
                                          this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * atomically remove elements from an array or has many relationship
     * @param path the path to get
     * @param primaryId id of the object with the relation
     * @param field name of the relation or array field to delete from
     * @param idToDelete id to atomically remove from field.
     *                   should be same type as the primary id of the related type (most likely String or Integer)
     * @param cascadeDelete true if related object specified in idToDelete should also be deleted
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public <T> void deleteIdFrom(String path,
                                 String primaryId,
                                 String field,
                                 T idToDelete,
                                 boolean cascadeDelete,
                                 StackMobRawCallback callback) {
        List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
        if (cascadeDelete) {
            headers.add(new Pair<String, String>("X-StackMob-CascadeDelete", "true"));
        }
        new StackMobRequestWithoutPayload(this.executor,
                                          this.session,
                                          HttpVerbWithoutPayload.DELETE,
                                          headers,
                                          StackMobRequest.EmptyParams,
                                          String.format("%s/%s/%s/%s", path, primaryId, field, idToDelete),
                                          callback,
                                          this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

    /**
     * retrieve the number of objects for a schema on the StackMob platform
     * @param path the path to get
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void count(String path, StackMobRawCallback callback) {
        count(new StackMobQuery(path), callback);
    }

    /**
     * retrieve the number of objects for a query on the StackMob platform
     * @param query the query to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void count(StackMobQuery query, StackMobRawCallback callback) {
        final StackMobRawCallback userCallback = callback;
        get(query.isInRange(0, 0), new StackMobRawCallback() {
            @Override
            public void unsent(StackMobException e) {
                userCallback.unsent(e);
            }
            @Override
            public void done(HttpVerb requestVerb, String requestURL, List<Map.Entry<String, String>> requestHeaders, String requestBody, Integer responseStatusCode, List<Map.Entry<String, String>> responseHeaders, byte[] responseBody) {
                if(Http.isSuccess(responseStatusCode)) {
                    long count = getTotalNumberOfItemsFromContentRange(responseHeaders);
                    if (count < 0) {
                        try { // No header means all available items were returned, so count them (0 or 1)
                            count = new JsonParser().parse(new String(responseBody)).getAsJsonArray().size();
                        } catch(Exception ignore) {}
                    }
                    responseBody = String.valueOf(count).getBytes();
                }
                userCallback.setDone(requestVerb, requestURL, requestHeaders, requestBody, responseStatusCode, responseHeaders, responseBody);
            }
        });
    }

}
