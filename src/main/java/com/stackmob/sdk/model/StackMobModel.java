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
import com.stackmob.sdk.api.StackMob;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.callback.StackMobIntermediaryCallback;
import com.stackmob.sdk.callback.StackMobNoopCallback;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.util.Pair;
import com.stackmob.sdk.util.RelationMapping;
import com.stackmob.sdk.util.SerializationMetadata;

import static com.stackmob.sdk.util.SerializationMetadata.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public abstract class StackMobModel {

    public class DateAsNumberTypeAdapter extends TypeAdapter<Date> {

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

    
    private transient String id;
    private transient Class<? extends StackMobModel> actualClass;
    private transient String schemaName;
    private transient boolean hasData;
    private transient Gson gson;

    public StackMobModel(String id, Class<? extends StackMobModel> actualClass) {
        this(actualClass);
        this.id = id;
    }

    public StackMobModel(Class<? extends StackMobModel> actualClass) {
        this.actualClass = actualClass;
        schemaName = actualClass.getSimpleName().toLowerCase();
        ensureValidModelName(schemaName);
        ensureMetadata(actualClass);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new DateAsNumberTypeAdapter());
        gson = gsonBuilder.create();
    }

    private void ensureValidFieldName(String name) {
        if(name.equalsIgnoreCase(getIDFieldName())) {
            throw new IllegalStateException(String.format("Don't create a field called %s. It's your object's id and is treated specially. Use setID and getID instead.", getIDFieldName()));
        }
        ensureValidName(name, "field");
    }

    private static void ensureValidModelName(String name) {
        if(name.contains("_")) throw new IllegalStateException("Model names may not contain underscore");
        ensureValidName(name,"model");
    }

    private static void ensureValidName(String name, String thing) {
        //The three character minimum isn't actually enforced for fields
        if(name.matches(".*(\\W).*") || name.length() > 25 || name.length() < 3) {
            throw new IllegalStateException(String.format("Invalid name for a %s: %s. Must be 3-25 alphanumeric characters", thing, name));
        }
    }

    private SerializationMetadata getMetadata(String fieldName) {
        return getSerializationMetadata(actualClass, fieldName);
    }

    private String getFieldName(String jsonName) {
        return getFieldNameFromJsonName(actualClass, jsonName);
    }
    
    public void setID(String id) {
        this.id = id;
    }
    
    public String getID() {
        return id;
    }

    protected void setActualClass(Class<? extends StackMobModel> actualClass) {
        this.actualClass = actualClass;
    }

    /**
     * Determines the schema connected to this class on the server. By
     * default it's the name of the class in lower case. Override in
     * subclasses to change that. Must be 3-25 alphanumeric characters.
     * @return the schema name
     */
    protected String getSchemaName() {
        return schemaName;
    }

    public String getIDFieldName() {
        return schemaName +"_id";
    }

    public boolean hasData() {
        return hasData;
    }
    
    protected void fillFieldFromJson(String jsonName, JsonElement json) throws StackMobException {
        try {
            if(jsonName.equals(getIDFieldName())) {
                // The id field is special, its name doesn't match the field
                setID(json.getAsJsonPrimitive().getAsString());
            } else {
                // undo the toLowerCase we do when sending out the json
                String fieldName = getFieldName(jsonName);
                if(fieldName != null) {
                    Field field = getField(fieldName);
                    field.setAccessible(true);
                    if(getMetadata(fieldName) == MODEL) {
                        // Delegate any expanded relations to the appropriate object
                        StackMobModel relatedModel = (StackMobModel) field.get(this);
                        // If there's a model with the same id, keep it. Otherwise create a new one
                        if(relatedModel == null || !relatedModel.hasSameID(json)) {
                            relatedModel = (StackMobModel) field.getType().newInstance();
                        }
                        relatedModel.fillFromJson(json);
                        field.set(this, relatedModel);
                    } else if(getMetadata(fieldName) == MODEL_ARRAY) {
                        Class<? extends StackMobModel> actualModelClass = (Class<? extends StackMobModel>) SerializationMetadata.getComponentClass(field);
                        Collection<StackMobModel> existingModels = getFieldAsCollection(field);
                        List<StackMobModel> newModels = updateModelListFromJson(json.getAsJsonArray(), existingModels, actualModelClass);
                        setFieldFromList(field, newModels, actualModelClass);
                    } else if(getMetadata(fieldName) == COUNTER) {
                        StackMobCounter counter = (StackMobCounter) field.get(this);
                        int newValue = json.getAsJsonPrimitive().getAsInt();
                        if(counter == null) {
                            counter = new StackMobCounter();
                            counter.set(newValue);
                            field.set(this, counter);
                        } else {
                            counter.set(newValue);
                        }
                    } else {
                        // Let gson do its thing
                        field.set(this, gson.fromJson(json, field.getType()));
                    }
                }
            }
        } catch(NoSuchFieldException e) {
            StackMob.getLogger().logDebug(String.format("Ignoring extraneous json field:\nfield: %s\ndata: %s", jsonName, json.toString()));
        } catch(JsonSyntaxException e) {
            StackMob.getLogger().logWarning(String.format("Incoming data does not match data model:\nfield: %s\ndata: %s", jsonName, json.toString()));
        } catch(IllegalAccessException e) {
            throw new StackMobException(e.getMessage());
        } catch (InstantiationException e) {
            throw new StackMobException(e.getMessage());
        }
    }

    /**
     * Turns a field which is either an Array or Collection of StackMobModels and turns in into a collection
     */
    protected Collection<StackMobModel> getFieldAsCollection(Field field) throws IllegalAccessException {
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
    protected void setFieldFromList(Field field, List<? extends StackMobModel> list, Class<? extends StackMobModel> modelClass) throws IllegalAccessException, InstantiationException {
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
    
    protected static List<StackMobModel> updateModelListFromJson(JsonArray array, Collection<? extends StackMobModel> existingModels, Class<? extends StackMobModel> modelClass) throws IllegalAccessException, InstantiationException, StackMobException {
        List<StackMobModel> result = new ArrayList<StackMobModel>();
        for(JsonElement json : array) {
            StackMobModel model = getExistingModel(existingModels, json);
            if(model == null) model = modelClass.newInstance();
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
    protected static StackMobModel getExistingModel(Collection<? extends StackMobModel> oldList, JsonElement json) {
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
                return classToCheck.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) { }
            classToCheck = classToCheck.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }
    
    public void fillFromJson(String jsonString) throws StackMobException {
        fillFromJson(new JsonParser().parse(jsonString));
    }

    protected void fillFromJson(JsonElement json) throws StackMobException {
        fillFromJson(json, null);
    }
    protected void fillFromJson(JsonElement json, List<String> selection) throws StackMobException {
        if(json.isJsonPrimitive()) {
            //This ought to be an unexpanded relation then
            setID(json.getAsJsonPrimitive().getAsString());
        } else {
            for (Map.Entry<String, JsonElement> jsonField : json.getAsJsonObject().entrySet()) {
                if(selection == null || selection.contains(jsonField.getKey())) {
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
    protected boolean hasSameID(JsonElement json) {
        if(getID() == null) return false;
        if(json.isJsonPrimitive()) {
            return getID().equals(json.getAsJsonPrimitive().getAsString());
        }
        JsonElement idFromJson = json.getAsJsonObject().get(getIDFieldName());
        return idFromJson != null && getID().equals(idFromJson.getAsString());
    }
    
    protected void setID(JsonElement json) {
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

    private JsonElement toJsonElement(int depth, RelationMapping mapping) {
        // Set the id here as opposed to on the server to avoid a race condition
        if(getID() == null) setID(UUID.randomUUID().toString().replace("-",""));
        if(depth < 0) return new JsonPrimitive(getID());
        JsonObject json = gson.toJsonTree(this).getAsJsonObject();
        JsonObject outgoing = new JsonObject();
        for(String fieldName : getFieldNames(json)) {
            ensureValidFieldName(fieldName);
            JsonElement value = json.get(fieldName);
            if(getMetadata(fieldName) == MODEL) {
                json.remove(fieldName);
                try {
                    Field relationField = getField(fieldName);
                    relationField.setAccessible(true);
                    StackMobModel relatedModel = (StackMobModel) relationField.get(this);
                    mapping.add(fieldName,relatedModel.getSchemaName());
                    JsonElement relatedJson = relatedModel.toJsonElement(depth - 1, mapping);
                    mapping.leave();
                    if(relatedJson != null) json.add(fieldName, relatedJson);
                } catch (Exception ignore) { } //Should never happen
            } else if(getMetadata(fieldName) == MODEL_ARRAY) {
                json.remove(fieldName);
                try {
                    Field relationField = getField(fieldName);
                    relationField.setAccessible(true);
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
                            mapping.add(fieldName,relatedModel.getSchemaName());
                            first = false;
                        }
                        JsonElement relatedJson = relatedModel.toJsonElement(depth - 1, mapping);
                        if(relatedJson != null) array.add(relatedJson);
                    }
                    if(!first) mapping.leave();
                    json.add(fieldName, array);
                } catch (Exception ignore) { } //Should never happen
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
                            fieldName += "[inc]";
                            json.add(fieldName, new JsonPrimitive(counter.getIncrement()));
                            break;
                        }
                        case SET: json.add(fieldName, new JsonPrimitive(counter.get())); break;
                    }
                    counter.reset();

                } catch (Exception ignore) { } //Should never happen
            }
            outgoing.add(fieldName.toLowerCase(), json.get(fieldName));
        }
        if(id != null) {
            outgoing.addProperty(getIDFieldName(),id);
        }
        return outgoing;
    }
    
    public String toJson() {
        return toJsonWithDepth(0);
    }
    
    public String toJsonWithDepth(int depth) {
        return toJsonWithDepth(depth, new RelationMapping());
    }

    /**
     * Converts the object to JSON turning all Models into their ids
     * @return the json representation of this model
     */
    protected String toJsonWithDepth(int depth, RelationMapping mapping) {
        return toJsonElement(depth, mapping).toString();
    }
    
    public void fetch() {
        fetch(new StackMobNoopCallback());
    }

    public void load(int depth) {
        fetchWithDepth(depth, new StackMobNoopCallback());
    }

    public void fetch(StackMobCallback callback) {
        fetchWithDepth(0, callback);
    }
    
    public void fetchWithDepth(int depth, StackMobCallback callback) {
        Map<String,String> args = new HashMap<String, String>();
        if(depth > 0) args.put("_expand", String.valueOf(depth));
        Map<String,String> headers = new HashMap<String, String>();
        StackMob.getStackMob().get(getSchemaName() + "/" + id, args, headers , new StackMobIntermediaryCallback(callback) {
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
    
    public void save() {
        save(new StackMobNoopCallback());
    }

    public void saveWithDepth(int depth) {
        saveWithDepth(depth, new StackMobNoopCallback());
    }

    public void save(StackMobCallback callback) {
        saveWithDepth(0, callback);
    }

    public void saveWithDepth(int depth, StackMobCallback callback) {
        RelationMapping mapping = new RelationMapping();
        String json = toJsonWithDepth(depth, mapping);
        List<Map.Entry<String,String>> headers= new ArrayList<Map.Entry<String,String>>();
        headers.add(new Pair<String,String>("X-StackMob-Relations", mapping.toHeaderString()));
        StackMob.getStackMob().post(getSchemaName(), json, headers, new StackMobIntermediaryCallback(callback) {
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

    public void destroy() {
        destroy(new StackMobNoopCallback());
    }

    public void destroy(StackMobCallback callback) {
        StackMob.getStackMob().delete(getSchemaName(), id, callback);
    }

}
