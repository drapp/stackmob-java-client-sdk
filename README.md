# StackMob Java Client SDK

With the StackMob Java Client SDK, you can integrate StackMob into any Java / JVM application.

Here are some example usages:

* Connect your Android app to your StackMob app (there is also an [Android SDK](https://github.com/stackmob/Stackmob_Android) that provides additional Android specific functionality)
* Connect your Java command line utility to your StackMob app
* Connect your Tomcat, JBoss, etc... app to your StackMob app

Hopefully you can see the pattern here. With this library, you can connect almost any JVM to your StackMob app and access the same app data as with the [iOS](https://github.com/stackmob/stackmob-ios-sdk), [Android](https://github.com/stackmob/Stackmob_Android) and [Ruby](https://github.com/stackmob/stackmob-ruby) SDKs.

# Getting Started

## With Maven

```xml
<dependency>
    <groupId>com.stackmob</groupId>
    <artifactId>stackmob-java-client-sdk</artifactId>
    <version>1.0.1</version>
</dependency>
```

## With SBT

```scala
libraryDependencies += "com.stackmob" % "stackmob-java-client-sdk" % "1.0.1"
```

## Commandline (or Ant)

Download the [StackMob Java SDK](http://search.maven.org/remotecontent?filepath=com/stackmob/stackmob-java-client-sdk/1.0.1/stackmob-java-client-sdk-1.0.1.jar) and the dependencies listed below and place them on your CLASSPATH:

* [Gson](http://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.1/gson-2.1.jar)
* [Scribe](http://search.maven.org/remotecontent?filepath=org/scribe/scribe/1.2.3/scribe-1.2.3.jar)
* [Apache Commons Codec](http://search.maven.org/remotecontent?filepath=commons-codec/commons-codec/1.4/commons-codec-1.4.jar)

## Android

This [StackMob Android SDK](https://developer.stackmob.com/sdks/android/config) is based on the Java SDK, and aside from setup they function identically. Check out the [Android Setup Tutorial](https://developer.stackmob.com/sdks/android/config) to get started.


# Using the SDK

* Check out the full list of [tutorials](https://developer.stackmob.com/tutorials/android). They're labeled as Android, but they also apply to the Java SDK
* Read the [javadocs](http://stackmob.github.com/stackmob-java-client-sdk/javadoc/apidocs/)

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
