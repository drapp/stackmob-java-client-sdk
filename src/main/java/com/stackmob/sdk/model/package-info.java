/**
 * Base classes for objects that map to StackMob data. Subclass {@link StackMobModel} or {@link StackMobUser} to create
 * objects that know how to synchronize themselves with the cloud. This creates a very powerful abstraction where you
 * can use data objects throughout your application and save them without worrying about the details of serialization and network
 * interaction.
 * @see StackMobModel
 * @see StackMobUser
 */
package com.stackmob.sdk.model;