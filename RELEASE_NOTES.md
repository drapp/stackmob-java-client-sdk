# Stackmob Java SDK (Release Notes)

### 0.4.3 (05/02/2012)
* Count methods
* Null and Not Equal Queries
* Logged in user checks
* User Model

### 0.4.2 (04/02/2012)
* Add optional logging

### 0.4.1 (03/30/2012)
* Restore the minimum android sdk version to 2.2

### 0.4.0 (03/28/2012)
* Model API

### 0.3.4 (03/05/2012)
* Select support in queries
* Persistent cookie support
* Fixed user-agent
* Minor bug fixes

### 0.3.3 (02/23/2012)
* Factored out cookie management
* Forgot/reset password support
* Minor bug fixes

### 0.3.2 (12/16/2011)
* Added support for android binary file upload
* Refactored StackMobCallback into StackMobRawCallback

### 0.3.0 (12/13/2011)
* Added GeoPoint support and Geospatial NEAR and WITHIN queries
* Advanced Relations support: post related object(s), add/delete items to relatioships and array-type fields
* Improved session cookie support
* Minor bug fixes

### 0.2.0 (11/3/2011)
* Added support for functionality like ORDER BY, LIMIT and SKIP in queries
* Moved expand to headers
* Added rudimentary support for binary file uploads
* Added more complete error checking when requests are complete - the callback failure method will be called now when HTTP 200 is returned but there's an error in the JSON
* Removed CookieManager based cookie handling, which is android 2.2 compatible

### 0.1.7 (10/27/2011)
* Added change to start using new-style (push.mob1.stackmob.com) push URLs

### 0.1.6 (10/25/2011)
* Added functionality to do HTTP requests in the background. callbacks (both redirect and normal) will now be called in a different thread than the original function call

### 0.1.5 (10/20/2011)
* Added the StackMobQuery class to assist with building complex query operations (ie: <, >, <=, =>, IN)
	* See [Javadoc](http://stackmob.github.com/stackmob-java-client-sdk/javadoc/0.1.5/apidocs) and [README](https://github.com/stackmob/stackmob-java-client-sdk/blob/master/README.md) for more details
* Overhaul of OAuth signing process & removed httpclient & signpost dependencies
* Fixed bug with login where it was not correctly saving the login cookie
* Changed StackMobRedirectCallback interface. See [Javadoc](http://stackmob.github.com/stackmob-java-client-sdk/javadoc/0.1.5/apidocs/com/stackmob/sdk/callback/StackMobRedirectedCallback.html) for more.

### 0.1.4 (10/17/2011)
* StackMob Push REST API support

### 0.1.3 (10/13/2011)
* Android compatability fixes
* Fixed SSL hostname verification issues for HTTPS
* Simplified redirect handling

### 0.1.1 (10/6/2011)
* Idential functionality to 0.1.0. this release was done to correct a problem with the previous release

### 0.1.0 (10/6/2011)
* Initial version of the StackMob Java SDK
  * Basic GET, POST, PUT, DELETE functionality
  * login/logout functionality
  * Twitter & Facebook functionality
  * Ability to follow cluster redirects given by the StackMob platform
  * Ability to cache cluster redirects given by the StackMob platform



