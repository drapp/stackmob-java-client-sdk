# StackMob Java Client SDK

With the StackMob Java Client SDK, you can integrate StackMob into any Java / JVM application.

Here are some example usages:

* Connect your Android app to your StackMob app (there is also an [Android SDK](https://github.com/stackmob/Stackmob_Android) that provides additional Android specific functionality)
* Connect your Java command line utility to your StackMob app
* Connect your Tomcat, JBoss, etc... app to your StackMob app

Hopefully you can see the pattern here. With this library, you can connect almost any JVM to your StackMob app and access the same app data as with the [iOS](https://github.com/stackmob/StackMob_iOS), [Android](https://github.com/stackmob/Stackmob_Android) and [Ruby](https://github.com/stackmob/stackmob-ruby) SDKs.

# Getting Started

## With Maven

```xml
<dependency>
    <groupId>com.stackmob</groupId>
    <artifactId>stackmob-java-client-sdk</artifactId>
    <version>0.5.0</version>
</dependency>
```

## With SBT

```scala
libraryDependencies += "com.stackmob" % "stackmob-java-client-sdk" % "0.5.0"
```

## Commandline (or Ant)

Download the [StackMob Java SDK](http://search.maven.org/remotecontent?filepath=com/stackmob/stackmob-java-client-sdk/0.5.0/stackmob-java-client-sdk-0.5.0.jar) and the dependencies listed below and place them on your CLASSPATH:

* [Gson](http://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.1/gson-2.1.jar)
* [Scribe](http://search.maven.org/remotecontent?filepath=org/scribe/scribe/1.2.3/scribe-1.2.3.jar)
* [Apache Commons Codec](http://search.maven.org/remotecontent?filepath=commons-codec/commons-codec/1.4/commons-codec-1.4.jar)

# Making REST API Calls

The main interface to your app on StackMob's servers is through the ``` com.stackmob.sdk.api.StackMob ``` object. Check out the [javadoc](http://stackmob.github.com/stackmob-java-client-sdk/javadoc/0.5.0/apidocs/) for details.
The following code shows basic use of the StackMob object.

```java
import com.stackmob.sdk.api.StackMob;
import com.stackmob.sdk.exception.StackMobException;

final String API_KEY = "YOUR API KEY HERE";
final String API_SECRET = "YOUR API SECRET HERE";
//leave this as a blank string if you don't have a user object.
//if you leave it blank, however, you must not call login, logout or any of the twitter or facebook methods,
//so we highly recommend that you set up a user object
final String USER_OBJ_NAME = "users";
//0 for sandbox, 1 or higher for a deployed API
final Integer API_VERSION = 0;
StackMob stackmob = new StackMob(API_KEY, API_SECRET, USER_OBJ_NAME, API_VERSION);

class MyObject {
    public String primary_key;
    public long createdDate;
    public long lastModDate;
    public String objectName;

    public MyObject(String n) {
        this.objectName = n;
    }
}

MyObject object = new MyObject("test object");

//create an object
stackmob.post("myobject", object, new StackMobCallback() {
    @Override public void success(String responseBody) {
        //handle the successul set
    }
    @Override public void failure(StackMobException e) {
        //handle the failure
    }
});
```

# Advanced Queries

The SDK includes `StackMobQuery` and `StackMobQueryWithField` classes to make building large queries easier than building up a Map of parameters. Here's how to use them.

## Expanding Relationships

If your object has a relationship field, typically the related objects' IDs are returned as an array.  You can return an array of objects instead by defining `expandDepthIs(..)`:

```java
StackMobQuery q = new StackMobQuery("user").expandDepthIs(1)
```

Below are JSON results without and with `expandDepthIs` respectively:

```json
//without expandDepthIs
{
  "username": "johnsmith",
  "age": 30,
  "friends": ["johndoe", "maryjane"]
}

//with expandDepthIs
{
  "username": "johnsmith",
  "age": 30,
  "friends": [
    {
      "username": "johndoe",
      "age": 35,
      ...
    },
    {
      "username": "maryjane",
      "age": 25",
      ...
    }]
}
```

Read more about <a href="https://www.stackmob.com/platform/stackmob/help/topics/Object-Relationships#a-expanding_relationships:_the__expand_parameter" target="_blank">Expanding Relationships</a>


## Querying for Multiple Values

Get an object with `username` equals to "johndoe1" or "johndoe2":

```java
StackMobQuery q = new StackMobQuery("user")
    .field("username").in(Arrays.asList("johndoe1", "johndoe2"))
	.getQuery();
```

Querying for an array field will also match on the contents of the array.  The following will return `users` who're friends with "johndoe" OR "maryjane":

```java
StackMobQuery q = new StackMobQuery("user")
    .field("friends").in(Arrays.asList("johndoe", "maryjane"))
	.getQuery();
```


## Inequality Queries

The StackMob API allows you to perform range queries using the `less than`, `less than or equal to`, `greater than`, and `greater than or equal to` operators.

Here's a query for an object with age less than "21":

```java
StackMobQuery q = new StackMobQuery("user")
    .field("age").isLessThan(21)
	.getQuery();
```

Here's a query for an object with age greater than "20" and less than or equal to "25":

```java
StackMobQuery q = new StackMobQuery("user")
    .field("age").isGreaterThan(20).isLessThanOrEqualTo(25)
	.getQuery();
```

## ORDER BY Queries

StackMobQuery also gives the ability to order results, similar to SQL's ORDER BY. Here's a query that orders users by when they joined:

```java
StackMobQuery q = new StackMobQuery("user")
	.field("createddate").isOrderedBy(StackMobQuery.Ordering.DESCENDING)
	.getQuery();
```

## Range Queries

You can also perform range queries similar to those you could perform in SQL using LIMIT and OFFSET. Here's code that builds on ORDER BY to get the 10 most recent users that joined:

```java
StackMobQuery q = new StackMobQuery("user")
	.field("createddate").isOrderedBy(StackMobQuery.Ordering.DESCENDING)
	.getQuery().isInRange(0, 9); //this range is inclusive
```

## Performing Requests using StackMobQuery

Now let's use `StackMobQuery` to make a REST API call.  This query represents all `myobject` objects named "object1" or "object2" that were created between 10 and 50 milliseconds ago (inclusive)


```java
long curTime = System.currentTimeMillis();
StackMobQuery q = new StackMobQuery("myobject")
    .field("objectName").in(Arrays.asList("object1", "object2"))
    .field("createddate").isGreaterThanOrEqualTo(curTime - 50).isLessThanOrEqualTo(curTime - 10);

stackmob.get(q, new StackMobCallback() {
    @Override public void success(String responseBody) {
        //responseBody will be a list of MyObject instances
        Type myObjectListType = new TypeToken<List<MyObject>>() {}.getType();
        List<MyObject> objects = gson.fromJson(responseBody, collectionType);
        //do something with your objects
    }
    @Override public void failure(StackMobException e) {
        //handle the failure
    }
});
```

# Issues
We use Github to track issues with the SDK. If you find any issues, please report them [here](https://github.com/stackmob/stackmob-java-client-sdk/issues), and include as many details as possible about the issue you encountered.

## Contributing
We encourage contributions to the StackMob SDK. If you'd like to contribute, fork this repository and make your changes. Before you submit a pull request to us with your changes, though, please keep the following in mind:

1. Please be sure that your code runs on Android 2.2 and above.
2. Please be sure to test your code against live StackMob servers. To do, make sure to set the STACKMOB_KEY and STACKMOB_SECRET env variables (or JVM vars) to your app's key & secret
3. If your tests must run with a specific server configuration (ie: specific object model, etc...), please include a descr


# Copyright

Copyright 2011 StackMob

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
