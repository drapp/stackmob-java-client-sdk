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

package com.stackmob.sdk.model;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.stackmob.sdk.api.*;
import com.stackmob.sdk.callback.*;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.util.Pair;
import com.stackmob.sdk.util.TypeHints;
import com.stackmob.sdk.util.SerializationMetadata;

import static com.stackmob.sdk.util.SerializationMetadata.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * The base class for StackMob data objects. Extend this class with the fields you want, and you have an object that knows how to synchronize itself with the cloud
 *
 * <pre>
 * {@code
 * public class Task extends StackMobModel {
 *     private String name;
 *     private Date dueDate;
 *     private int priority;
 *     private boolean done;
 *
 *     public Task(String name) {
 *         super(Task.class);
 *         this.name = name;
 *     }
 *
 *     //Add whatever setters/getters/other functionality you want here
 * }
 * }
 * </pre>
 *
 * You can then create objects, manipulate them, and save/load them whenever you want
 *
 * <pre>
 * {@code
 * Task myTask = new Task("write javadocs");
 * myTask.save();
 *
 * // ... do other stuff
 *
 * myTask.fetch(new StackMobModelCallback() {
 *     public void success() {
 *         // The blogPostTask object is now filled in with data.
 *     }
 *
 *     public void failure(StackMobException e) {
 *         // handle failure case
 *     }
 * });
 * }
 * </pre>
 *
 * You can also do complex queries and get model classes back as a result:
 * <pre>
 * {@code
 * // Add constraints
 * StackMobModelQuery<Task> highPriorityQuery = new StackMobModelQuery<Task>(Task.class).field(new StackMobField("priority").isGreaterThanOrEqualTo( 3).isLessThan(6)).fieldIsEqualTo("done", false);
 *
 * // Do an actual query
 * Task.query(highPriorityQuery, new StackMobQueryCallback<Task>() {
 *     public void success(List<Task> result) {
 *         // handle success
 *     }
 *
 *     public void failure(StackMobException e) {
 *         // handle failure
 *     }
 * });
 * }
 * </pre>
 *
 * If you use model classes as fields, you end up with a tree-like structure that you can save/load to any depth
 *
 * <pre>
 * {@code
 * public class TaskList extends StackMobModel {
 *     private String name;
 *     private List<Task> tasks = new ArrayList<Task>();
 *     public TaskList(String name, List<Task> tasks) {
 *         super(TaskList.class);
 *         this.name = name;
 *         this.tasks = tasks;
 *     }
 * }
 *
 * Task javadocsTask = new Task("Write javadocs");
 * Task proofreadTask = new Task("Proofread");
 * TaskList blogTasks = new TaskList("Blog Tasks", Arrays.asList(blogPostTask, proofreadTask));
 * blogTasks.save(StackMobOptions.depthOf(1));
 * }
 * </pre>
 *
 * Fields in a model can be any of the following:
 *
 * <ul>
 * <li> Java primitives/Strings</li>
 * <li> java.util.Date, java.math.BigInteger, java.math.BigDecimal</li>
 * <li> {@link StackMobCounter}
 * <li> Classes extending StackMobModel</li>
 * <li> Arrays and java Collections of the above</li>
 * <li> {@link StackMobFile} and {@link com.stackmob.sdk.api.StackMobGeoPoint} (these must be set up in the schema first)</li>
 * </ul>
 *
 *
 * Models are not inherently thread-safe; since they're just objects there's
 * nothing to stop you from accessing/modifying fields while fetch is in the
 * middle of updating them, or modifying an array field from different threads
 * and overwriting yourself. It's up to you to use standard thread-safety
 * procedures when dealing with models like you would with any java object.
 *
 *
 * Class and field names must be alphanumeric (no underscores) and at least three characters. If your model class has any required initialization it should happen in a zero args constructor. When
 * objects are created during queries and fetches field initialization may not happen, and other constructors may not be called.
 *
 * By default the name of the schema corresponding to your model on the server is the class name lowercased. To change this,
 * create the following static method in your subclass:
 * <pre>
 * {@code
 * public static String overrideSchemaName() {
 *     return "thenameyouwant";
 * }
 * }
 * </pre>
 *
 *
 */
public abstract class StackMobModel {

    /**
     * run a query on the server to get all the instances of your model within certain constraints
     * @param theClass The class of your model
     * @param q The query to run
     * @param callback The callback to be invoked upon returning
     */
    public static <T extends StackMobModel> void query(final Class<T> theClass, StackMobQuery q, final StackMobQueryCallback<T> callback) {
        query(theClass, q, new StackMobOptions(), callback);
    }

    /**
     * run a query on the server to get all the instances of your model within certain constraints
     * @param stackmob The stackmob instance to run requests on
     * @param theClass The class of your model
     * @param q The query to run
     * @param callback The callback to be invoked upon returning
     */
    public static <T extends StackMobModel> void query(StackMob stackmob, final Class<T> theClass, StackMobQuery q, final StackMobQueryCallback<T> callback) {
        query(stackmob, theClass, q, new StackMobOptions(), callback);
    }


    private static <T extends StackMobModel> String getSchemaName(Class<T> theClass) {
        try {
            Method getSchemaName = theClass.getDeclaredMethod("overrideSchemaName", new Class[]{});
            Object result = getSchemaName.invoke(null);
            return (String) result;
        } catch (Exception e) {
            return theClass.getSimpleName().toLowerCase();
        }
    }
    /**
     * run a query on the server to get all the instances of your model within certain constraints
     * @param theClass The class of your model
     * @param q The query to run
     * @param options options, such as select and expand, to apply to the request
     * @param callback The callback to be invoked upon returning
     */
    public static <T extends StackMobModel> void query(final Class<T> theClass, StackMobQuery q, StackMobOptions options, final StackMobQueryCallback<T> callback) {
        query(StackMob.getStackMob(), theClass, q, options, callback);
    }
    /**
     * run a query on the server to get all the instances of your model within certain constraints
     * @param stackmob The stackmob instance to run requests on
     * @param theClass The class of your model
     * @param q The query to run
     * @param options options, such as select and expand, to apply to the request
     * @param callback The callback to be invoked upon returning
     */
    public static <T extends StackMobModel> void query(final StackMob stackmob, final Class<T> theClass, StackMobQuery q, StackMobOptions options, final StackMobQueryCallback<T> callback) {
        q.setObjectName(getSchemaName(theClass));
        stackmob.getDatastore().get(q, options, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                JsonArray array = new JsonParser().parse(responseBody).getAsJsonArray();
                List<T> resultList = new ArrayList<T>();
                for(JsonElement elt : array) {
                    try {
                        resultList.add(StackMobModel.newFromJson(stackmob, theClass, elt.toString()));
                    } catch (StackMobException ignore) { }
                }
                callback.success(resultList);
            }

            @Override
            public void failure(StackMobException e) {
                callback.failure(e);
            }
        });
    }

    /**
     * run a count query on the server to count all the instances of your model within certain constraints
     * @param theClass The class of your model
     * @param q The query to run
     * @param callback The callback to be invoked upon returning
     */
    public static <T extends StackMobModel> void count(Class<T> theClass, StackMobQuery q, StackMobCountCallback callback) {
        count(StackMob.getStackMob(), theClass, q, callback);
    }

    /**
     * run a count query on the server to count all the instances of your model within certain constraints
     * @param theClass The class of your model
     * @param q The query to run
     * @param callback The callback to be invoked upon returning
     */
    public static <T extends StackMobModel> void count(StackMob stackmob, Class<T> theClass, StackMobQuery q, StackMobCountCallback callback) {
        q.setObjectName(getSchemaName(theClass));
        stackmob.getDatastore().count(q, callback);
    }

    /**
     * run a delete request with query parameters on the server.
     *
     * warning! this has the ability to delete a substantial amount of data in one request. use with care!
     *
     * @param theClass The class of your model
     * @param q The query on which to match objects for deletion
     * @param callback The callback to be invoked upon finishing
     */
    public static <T extends StackMobModel> void delete(Class<T> theClass, StackMobQuery q, StackMobCountCallback callback) {
        delete(StackMob.getStackMob(), theClass, q, callback);
    }

    /**
     * run a delete request with query parameters on the server.
     *
     * warning! this has the ability to delete a substantial amount of data in one request. use with care!
     *
     * @param stackmob The stackmob instance to run requests on
     * @param theClass The class of your model
     * @param q The query on which to match objects for deletion
     * @param callback The callback to be invoked upon finishing
     */
    public static <T extends StackMobModel> void delete(StackMob stackmob, Class<T> theClass, StackMobQuery q, StackMobCallback callback) {
        String schemaName = getSchemaName(theClass);
        q.setObjectName(schemaName);
        stackmob.getDatastore().delete(q, callback);
    }

    /**
     * create a new instance of the specified model class from a json string. Useful if you've serialized a model class for some
     * reason and now want to deserialize it.
     * @param classOfT The class to instantiate
     * @param json The string to deserialize
     * @return A new instance of the class based on the json
     * @throws StackMobException
     */
    public static <T extends StackMobModel> T newFromJson(StackMob stackmob, Class<T> classOfT, String json) throws StackMobException {
        T newObject = newInstance(classOfT);
        newObject.stackmob = stackmob;
        newObject.fillFromJson(json);
        return newObject;
    }

    private static <T extends StackMobModel> T newInstance(Class<T> classOfT) {
        T newObject = new Gson().fromJson("{}", classOfT);
        newObject.init(classOfT);
        return newObject;
    }

    /**
     * save multiple objects in one batch. This is equivalent to calling save on each
     * @param models
     * @param callback
     * @param <T>
     */
    public static <T extends StackMobModel> void saveMultiple(List<T> models, StackMobCallback callback) {
        saveMultiple(StackMob.getStackMob(), models, callback);

    }

    /**
     * save multiple objects in one batch. This is equivalent to calling save on each
     * @param models
     * @param callback
     * @param <T>
     */
    public static <T extends StackMobModel> void saveMultiple(StackMob stackmob, List<T> models, StackMobCallback callback) {
        if(models.size() == 0) throw new IllegalArgumentException("Empty list");
        stackmob.getDatastore().post(models.get(0).getSchemaName(), toJsonArray(models), callback);

    }


    private static <T extends StackMobModel> String toJsonArray(List<T> models) {
        JsonArray array = new JsonArray();
        for(T model : models) {
            array.add(model.toJsonElement(0, new Selection(null), new TypeHints(), new TypeHints()));
        }
        return array.toString();
    }


    private static class DateAsNumberTypeAdapter extends TypeAdapter<Date> {

        @Override
        public void write(JsonWriter jsonWriter, Date date) throws IOException {
            if (date == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.value(date.getTime());
        }

        @Override
        public Date read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return new Date(jsonReader.nextLong());
        }
    }

    private static Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new DateAsNumberTypeAdapter());
        gsonBuilder.registerTypeAdapter(StackMobForgotPasswordEmail.class, new StackMobForgotPasswordEmail.Deserializer());
        gsonBuilder.registerTypeAdapter(StackMobForgotPasswordEmail.class, new StackMobForgotPasswordEmail.Serializer());
        return gsonBuilder.create();
    }

    
    protected transient String id;
    private transient Class<? extends StackMobModel> actualClass;
    private transient String schemaName;
    private transient boolean hasData;
    private transient StackMob stackmob = StackMob.getStackMob();
    private static final Gson gson = getGson();

    /**
     * Have this model use a specific StackMob instance when making requests. Useful if you want
     * to access more than one StackMob application from the same app
     * @param stackmob the StackMob instance
     */
    public void setStackMob(StackMob stackmob) {
        this.stackmob = stackmob;
    }

    /**
     * create a new model of the specified class with an id overriding the default, automatically
     * generated one. This should be called by the subclass constructor.
     * @param actualClass The subclass, specified because of type erasure
     */
    public StackMobModel(String id, Class<? extends StackMobModel> actualClass) {
        this(actualClass);
        this.id = id;
    }

    /**
     * create a new model of the specified class. This must be called by the subclass constructor.
     * @param actualClass The subclass, specified because of type erasure
     */
    public StackMobModel(Class<? extends StackMobModel> actualClass) {
        init(actualClass);
    }

    protected void init(Class<? extends StackMobModel> actualClass) {
        this.actualClass = actualClass;
        schemaName = getSchemaName(actualClass);
        ensureValidName(schemaName, "model");
        ensureMetadata(actualClass);
    }

    private void ensureValidFieldName(String name) {
        if(name.equalsIgnoreCase(getIDFieldName())) {
            throw new IllegalStateException(String.format("Don't create a field called %s. It's your object's id and is treated specially. Use setID and getID instead.", getIDFieldName()));
        }
        ensureValidName(name, "field");
    }

    private static void ensureValidName(String name, String thing) {
        if(name.length() > 25) {
            throw new IllegalStateException(String.format("Invalid name for a %s: %s. Must be less than 25 alphanumeric characters", thing, name));
        }
    }

    private SerializationMetadata getMetadata(String fieldName) {
        return getSerializationMetadata(actualClass, fieldName);
    }

    private String getFieldName(String jsonName) {
        return getFieldNameFromJsonName(actualClass, jsonName);
    }

    /**
     * set the object's id. The id is a special field used as a primary key for an object. If not
     * specified this will be automatically generated when the object is saved
     * @param id the primary key of the object
     */
    public void setID(String id) {
        this.id = id;
    }

    /**
     * get the object's id. This was either set manually or generated on save
     * @return the primary key of the object
     */
    public String getID() {
        return id;
    }

    /**
     * set the actual subclass of this model
     * @param actualClass the actual subclass
     */
    void setActualClass(Class<? extends StackMobModel> actualClass) {
        this.actualClass = actualClass;
    }

    /**
     * The name of the schema this object corresponds to on the server.
     * To override, create the following static method in your sublcass:
     *  * <pre>
     * {@code
     * public static String overrideSchemaName() {
     *     return "thenameyouwant";
     * }
     * }
     * </pre>
     * The static method is necessary for the name to be accessible from
     * static method By default it's the name of the class in lower case.
     * Must be less than 25 alphanumeric characters.
     * @return the schema name
     */
    protected String getSchemaName() {
        return schemaName;
    }

    /**
     * Determines the field name for the primary key on the server. By
     * default it's the name of the class in lower case plus "_id". Override
     * in subclasses to change that. Must be 3-25 alphanumeric characters
     * @return the id field name
     */
    public String getIDFieldName() {
        return schemaName +"_id";
    }

    /**
     * Check if the object has been loaded with data or if it's just a stub with an id.
     * Objects can end up as stubs if you load an object tree to less than its full depth. Objects
     * without data shouldn't be used except to check the id or fetch data
     * @return whether or not this object has data
     */
    public boolean hasData() {
        return hasData;
    }

    private void fillModel(Field field, JsonElement json) throws StackMobException, IllegalAccessException { // Delegate any expanded relations to the appropriate object
        StackMobModel relatedModel = (StackMobModel) field.get(this);
        // If there's a model with the same id, keep it. Otherwise create a new one
        if(relatedModel == null || !relatedModel.hasSameID(json)) {
            relatedModel = newInstance((Class<? extends StackMobModel>) field.getType());
        }
        relatedModel.fillFromJson(json);
        field.set(this, relatedModel);
    }

    private void fillModelArray(Field field, JsonElement json) throws InstantiationException, IllegalAccessException, StackMobException {
        Class<? extends StackMobModel> actualModelClass = (Class<? extends StackMobModel>) SerializationMetadata.getComponentClass(field);
        Collection<StackMobModel> existingModels = getFieldAsCollection(field);
        List<StackMobModel> newModels = updateModelListFromJson(json.getAsJsonArray(), existingModels, actualModelClass);
        setFieldFromList(field, newModels, actualModelClass);
    }

    private void fillCounter(Field field, JsonElement json) throws IllegalAccessException {
        StackMobCounter counter = (StackMobCounter) field.get(this);
        int newValue = json.getAsJsonPrimitive().getAsInt();
        if(counter == null) {
            counter = new StackMobCounter();
            counter.set(newValue);
            field.set(this, counter);
        } else {
            counter.set(newValue);
        }
    }

    private void fillFieldFromJson(String jsonName, JsonElement json) throws StackMobException {
        try {
            if(jsonName.equals(getIDFieldName())) {
                // The id field is special, its name doesn't match the field
                setID(json.getAsJsonPrimitive().getAsString());
            } else {
                // undo the toLowerCase we do when sending out the json
                String fieldName = getFieldName(jsonName);
                if(fieldName != null) {
                    Field field = getField(fieldName);
                    if(getMetadata(fieldName) == MODEL) {
                        fillModel(field, json);
                    } else if(getMetadata(fieldName) == MODEL_ARRAY) {
                        fillModelArray(field, json);
                    } else if(getMetadata(fieldName) == COUNTER) {
                        fillCounter(field, json);
                    } else if(getMetadata(fieldName) == BINARY) {
                        StackMobFile file = (StackMobFile) field.get(this);
                        String url = json.getAsJsonPrimitive().getAsString();
                        if(file == null) {
                            StackMobFile newFile = new StackMobFile(url);
                            field.set(this, newFile);
                        } else {
                            file.setS3Url(url);
                        }
                    } else {
                        // Let gson do its thing
                        field.set(this, gson.fromJson(json, field.getType()));
                    }
                }
            }
        } catch(NoSuchFieldException e) {
            stackmob.getSession().getLogger().logDebug(String.format("Ignoring extraneous json field:\nfield: %s\ndata: %s", jsonName, json.toString()));
        } catch(JsonSyntaxException e) {
            stackmob.getSession().getLogger().logWarning(String.format("Incoming data does not match data model:\nfield: %s\ndata: %s", jsonName, json.toString()));
        } catch(IllegalAccessException e) {
            throw new StackMobException(e.getMessage());
        } catch (InstantiationException e) {
            throw new StackMobException(e.getMessage());
        }
    }

    /**
     * Turns a field which is either an Array or Collection of StackMobModels and turns in into a collection
     */
    Collection<StackMobModel> getFieldAsCollection(Field field) throws IllegalAccessException {
        if(field.getType().isArray()) {
            // grab the existing collection/array if there is one. We want to reuse any existing objects.
            // Otherwise we might end up clobbering a full object with just an id.
            StackMobModel[] models = (StackMobModel[]) field.get(this);
            return models == null ? null : Arrays.asList(models);
        } else {
            return (Collection<StackMobModel>) field.get(this);
        }
    }

    /**
     * Sets a field which is either an Array or Collection of StackMobModels using a list
     */
    void setFieldFromList(Field field, List<? extends StackMobModel> list, Class<? extends StackMobModel> modelClass) throws IllegalAccessException, InstantiationException {
        // We want to reuse the existing collection if at all possible
        if(field.getType().isArray()) {
            StackMobModel[] modelArray = (StackMobModel[]) field.get(this);
            if(modelArray == null || modelArray.length != list.size()) {
                field.set(this, Array.newInstance(modelClass,list.size()));
                modelArray = (StackMobModel[]) field.get(this);
            }
            for(int i = 0; i < list.size(); i++) {
                modelArray[i] = list.get(i);
            }
        } else {
            Collection<StackMobModel> models = (Collection<StackMobModel>)field.get(this);
            if(models == null) {
                initWithNewCollection(field);
                models = (Collection<StackMobModel>)field.get(this);
            }
            try {
                models.clear();
            } catch(UnsupportedOperationException e) {
                initWithNewCollection(field);
            }
            models.addAll(list);
        }
    }
    
    private void initWithNewCollection(Field field) throws IllegalAccessException {
        // Given a null Collection, how to we find the right
        // concrete collection to use? There is no good way.
        // So let's at least use the same hack as gson.
        field.set(this,gson.fromJson("[]", field.getType()));
    }
    
    static List<StackMobModel> updateModelListFromJson(JsonArray array, Collection<? extends StackMobModel> existingModels, Class<? extends StackMobModel> modelClass) throws IllegalAccessException, InstantiationException, StackMobException {
        List<StackMobModel> result = new ArrayList<StackMobModel>();
        for(JsonElement json : array) {
            StackMobModel model = getExistingModel(existingModels, json);
            if(model == null) model = newInstance(modelClass);
            model.fillFromJson(json);
            result.add(model);
        }
        return result;
    }

    /**
     * Finds a model with the same id as the json
     * @param oldList The data in the object already
     * @param json
     * @return
     */
    static StackMobModel getExistingModel(Collection<? extends StackMobModel> oldList, JsonElement json) {
        if(oldList != null) {
            //First try to find the existing model in the list
            for(StackMobModel model : oldList) {
                if(model.hasSameID(json)) {
                    return model;
                }
            }
            //If none, pick the first one without an id
            for(StackMobModel model : oldList) {
                if(model.getID() == null) {
                    model.setID(json);
                    return model;
                }
            }
        }
        return null;
    }
    
    private Field getField(String fieldName) throws NoSuchFieldException {
        Class<?> classToCheck = actualClass;
        while(!classToCheck.equals(StackMobModel.class)) {
            try {
                Field field = classToCheck.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) { }
            classToCheck = classToCheck.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**
     * fill the objects fields in from a json string. This isn't necessary during normal usage of a model class, but can be useful
     * if you've had to serialize the class for some reason
     * @param jsonString a json string as produced by {@link #toJson()}
     * @throws StackMobException
     */
    public void fillFromJson(String jsonString) throws StackMobException {
        fillFromJson(new JsonParser().parse(jsonString));
    }

    void fillFromJson(JsonElement json) throws StackMobException {
        fillFromJson(json, null);
    }

    void fillFromJson(JsonElement json, List<String> selection) throws StackMobException {
        if(json.isJsonPrimitive()) {
            //This ought to be an unexpanded relation then
            setID(json.getAsJsonPrimitive().getAsString());
        } else {
            for (Map.Entry<String, JsonElement> jsonField : json.getAsJsonObject().entrySet()) {
                if(selection == null || selection.contains(jsonField.getKey()) || getMetadata(jsonField.getKey()) == BINARY) {
                    fillFieldFromJson(jsonField.getKey(), jsonField.getValue());
                }
            }
            hasData = true;
        }
    }

    /**
     * Checks if the current object has the same id as this json
     * @param json
     * @return
     */
    boolean hasSameID(JsonElement json) {
        if(getID() == null) return false;
        if(json.isJsonPrimitive()) {
            return getID().equals(json.getAsJsonPrimitive().getAsString());
        }
        JsonElement idFromJson = json.getAsJsonObject().get(getIDFieldName());
        return idFromJson != null && getID().equals(idFromJson.getAsString());
    }
    
    void setID(JsonElement json) {
        if(json.isJsonPrimitive()) {
            setID(json.getAsJsonPrimitive().getAsString());
        } else {
            setID(json.getAsJsonObject().get(getIDFieldName()).getAsString());
        }
    }
    
    private List<String> getFieldNames(JsonObject json) {
        List<String> list = new ArrayList<String>();
        for(Map.Entry<String,JsonElement> entry : json.entrySet()) {
            list.add(entry.getKey());
        }
        return list;
    }

    private void replaceModelJson(JsonObject json, String fieldName, Selection selection, TypeHints relationHints, TypeHints typeHints, int depth) {
        json.remove(fieldName);
        try {
            Field relationField = getField(fieldName);
            StackMobModel relatedModel = (StackMobModel) relationField.get(this);
            relationHints.add(fieldName, relatedModel.getSchemaName());
            relationHints.push(fieldName);
            typeHints.push(fieldName);
            JsonElement relatedJson = relatedModel.toJsonElement(depth - 1, selection.subSelection(fieldName), relationHints, typeHints);
            relationHints.pop();
            typeHints.pop();
            if(relatedJson != null) json.add(fieldName, relatedJson);
        } catch (Exception ignore) { } //Should never happen
    }

    private void replaceModelArrayJson(JsonObject json, String fieldName, Selection selection, TypeHints relationHints, TypeHints typeHints, int depth) {
        json.remove(fieldName);
        try {
            Field relationField = getField(fieldName);
            JsonArray array = new JsonArray();
            Collection<StackMobModel> relatedModels;
            if(relationField.getType().isArray()) {
                relatedModels = Arrays.asList((StackMobModel[])relationField.get(this));
            } else {
                relatedModels = (Collection<StackMobModel>) relationField.get(this);
            }
            boolean first = true;
            for(StackMobModel relatedModel : relatedModels) {
                if(first) {
                    relationHints.add(fieldName, relatedModel.getSchemaName());
                    relationHints.push(fieldName);
                    typeHints.push(fieldName);
                    first = false;
                }
                JsonElement relatedJson = relatedModel.toJsonElement(depth - 1, selection.subSelection(fieldName), relationHints, typeHints);
                if(relatedJson != null) array.add(relatedJson);
            }
            if(!first) {
                relationHints.pop();
                typeHints.pop();
            }
            json.add(fieldName, array);
        } catch (Exception ignore) { } //Should never happen
    }

    protected JsonElement toJsonElement(int depth, Selection selection, TypeHints relationHints, TypeHints typeHints) {
        // Set the id here as opposed to on the server to avoid a race condition
        if(getID() == null) setID(UUID.randomUUID().toString().replace("-",""));
        if(depth < 0) return new JsonPrimitive(getID());
        JsonObject json = gson.toJsonTree(this).getAsJsonObject();
        JsonObject outgoing = new JsonObject();
        for(String fieldName : getFieldNames(json)) {
            if(!selection.isSelected(fieldName)) continue;
            String newFieldName = fieldName;
            ensureValidFieldName(fieldName);
            JsonElement value = json.get(fieldName);
            if(getMetadata(fieldName) == MODEL) {
                replaceModelJson(json, fieldName, selection, relationHints, typeHints, depth);
            } else if(getMetadata(fieldName) == MODEL_ARRAY) {
                replaceModelArrayJson(json, fieldName, selection, relationHints, typeHints, depth);
            } else if(getMetadata(fieldName) == OBJECT) {
                //We don't support subobjects. Gson automatically converts a few types like
                //Date and BigInteger to primitive types, but anything else has to be an error.
                if(value.isJsonObject()) {
                    throw new IllegalStateException("Field " + fieldName + " is a subobject which is not supported at this time");
                }
            } else if(getMetadata(fieldName) == COUNTER) {
                json.remove(fieldName);
                try {
                    StackMobCounter counter = (StackMobCounter) getField(fieldName).get(this);
                    switch(counter.getMode()) {
                        case INCREMENT: {
                            newFieldName += "[inc]";
                            json.add(fieldName, new JsonPrimitive(counter.getIncrement()));
                            break;
                        }
                        case SET: json.add(fieldName, new JsonPrimitive(counter.get())); break;
                    }
                    counter.reset();

                } catch (Exception ignore) { } //Should never happen
            } else if(getMetadata(fieldName) == BINARY) {
                typeHints.add(fieldName, BINARY.name().toLowerCase());
                json.remove(fieldName);
                try {
                    StackMobFile file = (StackMobFile) getField(fieldName).get(this);
                    if(file.getBinaryString() != null) {
                        json.add(fieldName, new JsonPrimitive(file.getBinaryString()));
                    } else {
                        //don't post the url
                        newFieldName = null;
                    }

                } catch(Exception e) {
                    stackmob.getSession().getLogger().logWarning("Got exception while serializing binary file " + e);
                } //Should never happen
            } else if(getMetadata(fieldName) == GEOPOINT) {
                typeHints.add(fieldName, GEOPOINT.name().toLowerCase());
            } else if(getMetadata(fieldName) == FORGOT_PASSWORD) {
                typeHints.add(fieldName, FORGOT_PASSWORD.name().toLowerCase().replace("_", ""));
            }
            if(newFieldName != null) outgoing.add(newFieldName.toLowerCase(), json.get(fieldName));
        }
        if(id != null) {
            outgoing.addProperty(getIDFieldName(),id);
        }
        return outgoing;
    }

    /**
     * Converts the model into its Json representation. This method is used internally while communicating with the cloud, but can also come in handy anytime you need a string representation of your model objects, such as passing them around in Intents on Android.
     * @return a json representation of the object
     */
    public String toJson() {
        return toJson(StackMobOptions.none());
    }

    /**
     * Converts the model into its Json representation, expanding any sub-objects to the given depth. Be sure to use the right depth, or you'll end up with empty sub-objects.
     * @param options options, such and select and expand, to apply to the request
     * @return a json representation of the object and its children to the depth
     */
    public String toJson(StackMobOptions options) {
        return toJson(options, new TypeHints(), new TypeHints());
    }

    String toJson(StackMobOptions options, TypeHints relationHints, TypeHints typeHints) {
        return toJsonElement(options.getExpandDepth(), new Selection(options.getSelection()), relationHints, typeHints).toString();
    }

    /**
     * Reload the object from the server. This is not thread safe, make
     * sure the object isn't disturbed during the load.
     * @param callback invoked when the load is complete
     */
    public void fetch(StackMobCallback callback) {
        fetch(StackMobOptions.none(), callback);
    }

    /**
     * Reload the object from the server. Use {@link StackMobOptions#depthOf(int)} to also save its children to the given depth.
     * This is not thread safe, make sure the object isn't disturbed during the load.
     * @param options options, such and select and expand, to apply to the request
     * @param callback invoked when the load is complete
     */
    public void fetch(StackMobOptions options, StackMobCallback callback) {
        stackmob.getDatastore().get(getSchemaName() + "/" + id, options, new StackMobIntermediaryCallback(callback) {
            @Override
            public void success(String responseBody) {
                boolean fillSucceeded = false;
                try {
                    StackMobModel.this.fillFromJson(new JsonParser().parse(responseBody));
                    fillSucceeded = true;
                } catch (StackMobException e) {
                    failure(e);
                }
                if(fillSucceeded) super.success(responseBody);
            }
        });
    }

    /**
     * Save the object to the server
     */
    public void save() {
        save(new StackMobNoopCallback());
    }

    /**
     * Save the object to the server with options. Use {@link StackMobOptions#depthOf(int)} to also save its children to the given depth.
     * @param options options, such and select and expand, to apply to the request
     */
    public void save(StackMobOptions options) {
        save(options, new StackMobNoopCallback());
    }

    /**
     * Save the object to the server
     * @param callback invoked when the save is complete
     */
    public void save(StackMobCallback callback) {
        save(StackMobOptions.none(), callback);
    }

    /**
     * Save the object to the server with options. Use {@link StackMobOptions#depthOf(int)} to also save its children to the given depth.
     * @param options options, such and select and expand, to apply to the request
     * @param callback invoked when the save is complete
     */
    public void save(StackMobOptions options, StackMobCallback callback) {
        TypeHints relationHints = new TypeHints();
        TypeHints typeHints = new TypeHints();
        String json = toJson(options, relationHints, typeHints);
        List<Map.Entry<String,String>> headers= new ArrayList<Map.Entry<String,String>>();
        headers.add(new Pair<String,String>("X-StackMob-Relations", relationHints.toHeaderString()));
        headers.add(new Pair<String,String>("X-StackMob-FieldTypes", typeHints.toHeaderString()));
        stackmob.getDatastore().post(getSchemaName(), json, options.withHeaders(headers), new StackMobIntermediaryCallback(callback) {
            @Override
            public void success(String responseBody) {
                boolean fillSucceeded = false;
                try {
                    fillFromJson(new JsonParser().parse(responseBody), Arrays.asList("lastmoddate", "createddate"));
                    fillSucceeded = true;
                } catch (StackMobException e) {
                    failure(e);
                }
                if(fillSucceeded) super.success(responseBody);
            }
        });
    }

    /**
     * delete the object from the server
     */
    public void destroy() {
        destroy(new StackMobNoopCallback());
    }

    /**
     * delete the object from the server
     * @param callback invoked when the delete is complete
     */
    public void destroy(StackMobCallback callback) {
        stackmob.getDatastore().delete(getSchemaName(), id, callback);
    }

    public void exists(StackMobExistsCallback callback) {
        stackmob.getDatastore().head(getSchemaName() + "/" + id, callback);
    }


    private <T extends StackMobModel> List<String> getIdsFromModels(List<T> models) {
        List<String> ids = new ArrayList<String>();
        for(T model : models) {
            ids.add(model.id);
        }
        return ids;
    }

    /**
     * append model objects to a collection field in this object. The items must match the type of the array, or
     * an exception will be thrown. The objects should already exist on the server; to append and create
     * objects, use {@link #appendAndSave(String, java.util.List, com.stackmob.sdk.callback.StackMobCallback)}.
     * The items will be added to the list both locally and on the server.
     *
     * @throws IllegalArgumentException if the type of the field doesn't match the input objects
     * @param field the name of the field to append to. The field must be a java Collection
     * @param objs the objects to append to
     * @param callback invoked when the append is complete
     * @param <T> the type of objects being appended
     */
    public <T extends StackMobModel> void append(String field, List<T> objs, StackMobCallback callback) {
        try {
            Collection<T> existingCollection = (Collection<T>) getField(field).get(this);
            for(T obj : objs) {
                existingCollection.add(obj);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Type of input objects does not match the type of the field");
        }
        stackmob.getDatastore().putRelated(schemaName, id, field.toLowerCase(), getIdsFromModels(objs), callback);
    }

    /**
     * append model objects to a collection field in this object. The items must match the type of the array, or
     * an exception will be thrown. The objects will be created and also added as children of this object
     * both locally and on the server.
     *
     * @throws IllegalArgumentException if the type of the field doesn't match the input objects
     * @param field the name of the field to append to. The field must be a java Collection
     * @param objs the objects to append to
     * @param callback invoked when the append is complete
     * @param <T> the type of objects being appended
     */
    public <T extends StackMobModel> void appendAndSave(String field, List<T> objs, StackMobCallback callback) {
        try {
            Collection<T> existingCollection = (Collection<T>) getField(field).get(this);
            for(T obj : objs) {
                existingCollection.add(obj);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Type of input objects does not match the type of the field");
        }
        stackmob.getDatastore().postRelated(schemaName, id, field.toLowerCase(), toJsonArray(objs), callback);
    }

    /**
     * remove values from a collection on the client and server. The items must match the type of the array, or
     * an exception will be thrown.
     *
     * @throws IllegalArgumentException if the type of the field doesn't match the input objects
     * @param field the name of the field to remove from. The field must be a java Collection
     * @param objs the objects to remove from
     * @param callback invoked when the remove is complete
     * @param <T> the type of objects being removed
     */
    public <T extends StackMobModel> void remove(String field,  List<T> objs, StackMobCallback callback) {
        try {
            Collection<T> existingCollection = (Collection<T>) getField(field).get(this);
            for(T obj : objs) {
                existingCollection.remove(obj);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Type of input objects does not match the type of the field");
        }
        stackmob.getDatastore().deleteIdsFrom(schemaName, id, field.toLowerCase(), getIdsFromModels(objs), false, callback);

    }

    /**
     * remove objects from a collection and delete them on the client and server. The items must match the type of the array, or
     * an exception will be thrown.
     *
     * @throws IllegalArgumentException if the type of the field doesn't match the input objects
     * @param field the name of the field to remove from. The field must be a java Collection
     * @param objs the objects to remove from
     * @param callback invoked when the remove is complete
     * @param <T> the type of objects being removed
     */
    public <T extends StackMobModel> void removeAndDelete(String field,  List<T> objs, StackMobCallback callback) {
        try {
            Collection<T> existingCollection = (Collection<T>) getField(field).get(this);
            for(T obj : objs) {
                existingCollection.remove(obj);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Type of input objects does not match the type of the field");
        }
        stackmob.getDatastore().deleteIdsFrom(schemaName, id, field.toLowerCase(), getIdsFromModels(objs), true, callback);
    }

    /*
     * Handles selection logic for outgoing requests. The logic mirrors that of the server
     */
    private static class Selection {

        private List<String> fields;

        public Selection(List<String> fields) {
            this.fields = fields;
        }

        public boolean isSelected(String field) {
            return fields == null || fields.contains(field);
        }

        public Selection subSelection(String field) {
            if(fields == null) return new Selection(null);
            String prefix = field + ".";
            List<String> newSelection = new ArrayList<String>();
            for(String selection : fields) {
                if(selection.startsWith(prefix)) {
                    newSelection.add(selection.substring(prefix.length()));
                }
            }
            return new Selection(newSelection);
        }
    }
}
