## Stackmob Java SDK Change Log

### v1.1.2 --- Feb 14, 2013
* New push API

### v1.1.1 --- Jan 10, 2013
* Add support for per-request https override

### v1.1.0 --- Jan 9, 2013
* Add support for saving only selected fields
* Fix overriding model schema names in static method
* Fix outdated schema name restrictions

### v1.0.2 --- Dec 5, 2012
* Fix private StackMobFile objects
* Handle random nulls from scribe
* Ensure failure callbacks are called
* Other bugfixes

### v1.0.1 --- Oct 2, 2012
* Bugfixes

### v1.0.0 --- Sept 17, 2012
* Refactor for simplicity and parity with the iOS SDK
* GeoPoint and Binary File support for StackMobModel
* Auth and push methods for StackMobUser

### v0.5.6 --- Aug 22, 2012
* Retry callback for 503 errors
* Bugfixes

### v0.5.4 --- Jul 27, 2012
* Fix OAuth2 over ssl
* Fix pushToTokens for GCM

### v0.5.3 --- Jul 24, 2012
* Bugfixes

### v0.5.2 --- Jul 23, 2012
* Bugfixes

### v0.5.1 --- Jul 19, 2012
* GCM Push Support
* Refresh Tokens
* Fix oauth2 push bug
* Https override switch

### v0.5.0 --- Jul 10, 2012
* OAuth2 Support

### v0.4.7 --- Jun 27, 2012
* New push functions

### v0.4.6 --- May 31, 2012
* Bug fix for atomic increment

### v0.4.5 --- May 30, 2012
* Atomic Increment
* Overwrite flag for push
* Bugfixes

### v0.4.4 --- May 4, 2012
* Quick fix for shared StackMob instance problem

### v0.4.3 --- May 2, 2012
* Count methods
* Null and Not Equal Queries
* Logged in user checks
* User Model

### v0.4.2 --- April 2, 2012
* Add optional logging

### v0.4.1 --- Mar 30, 2012
* Restore the minimum android sdk version to 2.2

### v0.4.0 --- Mar 28, 2012
* Model API

### v0.3.4 --- Mar 5, 2012
* Select support in queries
* Persistent cookie support
* Fixed user-agent
* Minor bug fixes

### v0.3.3 --- Feb 23, 2012
* Factored out cookie management
* Forgot/reset password support
* Minor bug fixes

### v0.3.2 --- Dec 16, 2011
* Added support for android binary file upload
* Refactored StackMobCallback into StackMobRawCallback

### v0.3.0 --- Dec 13, 2011
* Added GeoPoint support and Geospatial NEAR and WITHIN queries
* Advanced Relations support: post related object(s), add/delete items to relatioships and array-type fields
* Improved session cookie support
* Minor bug fixes

### v0.2.0 --- Nov 3, 2011
* Added support for functionality like ORDER BY, LIMIT and SKIP in queries
* Moved expand to headers
* Added rudimentary support for binary file uploads
* Added more complete error checking when requests are complete - the callback failure method will be called now when HTTP 200 is returned but there's an error in the JSON
* Removed CookieManager based cookie handling, which is android 2.2 compatible

### v0.1.7 --- Oct 27, 2011
* Added change to start using new-style (push.mob1.stackmob.com) push URLs

### v0.1.6 --- Oct 25, 2011
* Added functionality to do HTTP requests in the background. callbacks (both redirect and normal) will now be called in a different thread than the original function call

### v0.1.5 --- Oct 20, 2011
* Added the StackMobQuery class to assist with building complex query operations (ie: <, >, <=, =>, IN)
	* See [Javadoc](http://stackmob.github.com/stackmob-java-client-sdk/javadoc/0.1.5/apidocs) and [README](https://github.com/stackmob/stackmob-java-client-sdk/blob/master/README.md) for more details
* Overhaul of OAuth signing process & removed httpclient & signpost dependencies
* Fixed bug with login where it was not correctly saving the login cookie
* Changed StackMobRedirectCallback interface. See [Javadoc](http://stackmob.github.com/stackmob-java-client-sdk/javadoc/0.1.5/apidocs/com/stackmob/sdk/callback/StackMobRedirectedCallback.html) for more.

### v0.1.4 ---  Oct 17, 2011
* StackMob Push REST API support

### v0.1.3 --- Oct 13, 2011
* Android compatability fixes
* Fixed SSL hostname verification issues for HTTPS
* Simplified redirect handling

### v0.1.1 --- Oct 6, 2011
* Idential functionality to 0.1.0. this release was done to correct a problem with the previous release

### v0.1.0 --- Oct 6, 2011
* Initial version of the StackMob Java SDK
  * Basic GET, POST, PUT, DELETE functionality
  * login/logout functionality
  * Twitter & Facebook functionality
  * Ability to follow cluster redirects given by the StackMob platform
  * Ability to cache cluster redirects given by the StackMob platform



