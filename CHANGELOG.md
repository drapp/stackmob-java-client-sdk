<h2>StackMob Java SDK Change Log</h2>

<h3>v1.3.0 - May 31, 2013</h3>

**Features**

* Added "not in" support
* Added HEAD request support

<h3>v1.2.1 - March 27, 2013</h3>

**Features**

* Create if necessary for Facebook and Twitter login 
* Token unlinking for Facebook and Twitter
* OR query support, allowing, for example, the ability to query "todo where A AND (B OR (C AND D) OR E)"
* Support for querying where field equals / does not equal the empty string.
* Support JDK 6 and JDK 7

**Fixes**

* Send pushToUsers to the correct endpoint
* Fix Facebook create method to ignore null usesrnames
* Change Facebook and Twitter create methods to POST request

<h3>v1.1.2 - February 14, 2013</h3>

**Features**

* New push API
* Gigya login support

<h3>v1.1.1 - January 10, 2013</h3>

**Features**

* Add support for per-request https override

<h3>v1.1.0 - January 9, 2013</h3>

**Features**

* Add support for saving only selected fields

**Fixes**

* Fix overriding model schema names in static method
* Fix outdated schema name restrictions

<h3>v1.0.2 - December 5, 2012</h3>

**Fixes**

* Fix private StackMobFile objects
* Handle random nulls from scribe
* Ensure failure callbacks are called
* Other bugfixes

<h3>v1.0.1 - October 2, 2012</h3>

**Fixes**

* Bugfixes

<h3> v1.0.0 - September 17, 2012</h3>

**Features**

* GeoPoint and Binary File support for StackMobModel
* Auth and push methods for StackMobUser

**Fixes**

* Refactor for simplicity and parity with the iOS SDK

<h3> v0.5.6 - August 22, 2012</h3>

**Features**

* Retry callback for 503 errors

**Fixes**

* Bugfixes

<h3> v0.5.4 - July 27, 2012</h3>

**Fixes**

* Fix OAuth2 over ssl
* Fix pushToTokens for GCM

<h3> v0.5.3 - July 24, 2012</h3>

**Fixes**

* Bugfixes

<h3> v0.5.2 - July 23, 2012</h3>

**Fixes**

* Bugfixes

<h3> v0.5.1 - July 19, 2012</h3>

**Features**

* GCM Push Support
* Refresh Tokens
* Https override switch

**Fixes**

* Fix oauth2 push bug

<h3> v0.5.0 - July 10, 2012</h3>

**Features**

* OAuth2 Support

<h3> v0.4.7 - June 27, 2012</h3>

**Features**

* New push functions

<h3> v0.4.6 - May 31, 2012</h3>

**Fixes**

* Bug fix for atomic increment

<h3> v0.4.5 - May 30, 2012</h3>

**Features**

* Atomic Increment
* Overwrite flag for push

**Fixes**

* Bugfixes

<h3> v0.4.4 - May 4, 2012</h3>

**Fixes**

* Quick fix for shared StackMob instance problem

<h3> v0.4.3 - May 2, 2012</h3>

**Features**

* Count methods
* Null and Not Equal Queries
* Logged in user checks
* User Model

<h3> v0.4.2 - April 2, 2012</h3>

**Features**

* Add optional logging

<h3> v0.4.1 - March 30, 2012</h3>

**Update Notes**

* Restore the minimum android sdk version to 2.2

<h3> v0.4.0 - March 28, 2012</h3>

**Features**

* Model API

<h3> v0.3.4 - March 5, 2012</h3>

**Features**

* Select support in queries
* Persistent cookie support

**Fixes**

* Fixed user-agent
* Minor bug fixes

<h3> v0.3.3 - Febuary 23, 2012</h3>

**Features**

* Factored out cookie management
* Forgot/reset password support

**Fixes**

* Minor bug fixes

<h3> v0.3.2 - December 16, 2011</h3>

**Features**

* Added support for android binary file upload

**Update Notes**

* Refactored StackMobCallback into StackMobRawCallback

<h3> v0.3.0 - December 13, 2011</h3>

**Features**

* Added GeoPoint support and Geospatial NEAR and WITHIN queries
* Advanced Relations support: post related object(s), add/delete items to relatioships and array-type fields
* Improved session cookie support

**Fixes**

* Minor bug fixes

<h3> v0.2.0 - November 3, 2011</h3>

**Features**

* Added support for functionality like ORDER BY, LIMIT and SKIP in queries
* Added rudimentary support for binary file uploads
* Added more complete error checking when requests are complete - the callback failure method will be called now when HTTP 200 is returned but there's an error in the JSON

**Update Notes**

* Moved expand to headers
* Removed CookieManager based cookie handling, which is android 2.2 compatible

<h3> v0.1.7 - October 27, 2011</h3>

**Update Notes**

* Added change to start using new-style (push.mob1.stackmob.com) push URLs

<h3> v0.1.6 - October 25, 2011</h3>

**Features**

* Added functionality to do HTTP requests in the background. callbacks (both redirect and normal) will now be called in a different thread than the original function call

<h3> v0.1.5 - October 20, 2011</h3>

**Features**

* Added the StackMobQuery class to assist with building complex query operations (ie: <, >, <=, =>, IN)
	* See [Javadoc](http://stackmob.github.com/stackmob-java-client-sdk/javadoc/0.1.5/apidocs) and [README](https://github.com/stackmob/stackmob-java-client-sdk/blob/master/README.md) for more details

**Fixes**

* Fixed bug with login where it was not correctly saving the login cookie

**Update Notes**

* Overhaul of OAuth signing process & removed httpclient & signpost dependencies
* Changed StackMobRedirectCallback interface. See [Javadoc](http://stackmob.github.com/stackmob-java-client-sdk/javadoc/0.1.5/apidocs/com/stackmob/sdk/callback/StackMobRedirectedCallback.html) for more.

<h3> v0.1.4 -  October 17, 2011</h3>

**Features**

* StackMob Push REST API support

<h3> v0.1.3 - October 13, 2011</h3>

**Fixes**

* Android compatability fixes
* Fixed SSL hostname verification issues for HTTPS
* Simplified redirect handling

<h3> v0.1.1 - October 6, 2011</h3>

**Update Notes**

* Identical functionality to 0.1.0. this release was done to correct a problem with the previous release

<h3> v0.1.0 - October 6, 2011</h3>

**Features**

* Initial version of the StackMob Java SDK
  * Basic GET, POST, PUT, DELETE functionality
  * login/logout functionality
  * Twitter & Facebook functionality
  * Ability to follow cluster redirects given by the StackMob platform
  * Ability to cache cluster redirects given by the StackMob platform



