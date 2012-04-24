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

package com.stackmob.sdk.api;

import com.stackmob.sdk.StackMobTestCommon;
import com.stackmob.sdk.api.StackMobFile;
import com.stackmob.sdk.api.StackMobQuery;
import com.stackmob.sdk.api.StackMobQueryWithField;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.concurrencyutils.CountDownLatchUtils;
import com.stackmob.sdk.concurrencyutils.MultiThreadAsserter;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.net.StackMobApi;
import com.stackmob.sdk.testobjects.Game;
import com.stackmob.sdk.testobjects.S3Object;
import com.stackmob.sdk.testobjects.StackMobObjectOnServer;
import com.stackmob.sdk.testobjects.User;
import org.junit.Test;
import org.scribe.services.TimestampService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static com.stackmob.sdk.concurrencyutils.CountDownLatchUtils.latchOne;

public class StackMobTests extends StackMobTestCommon {

    @Test public void login() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();
        final User user = new User(username, password);
        final StackMobObjectOnServer<User> objectOnServer = createOnServer(user, User.class);
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", user.username);
        params.put("password", user.password);

        final CountDownLatch latch = latchOne();

        stackmob.login(params, new StackMobCallback() {
            @Override public void success(String responseBody) {
                asserter.markNotNull(responseBody);
                asserter.markNotJsonError(responseBody);
                latch.countDown();
            }

            @Override public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        objectOnServer.delete();
    }

    @Test public void loginShouldFail() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("password", password);

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.login(params, new StackMobCallback() {
            @Override public void success(String responseBody) {
                asserter.markJsonError(responseBody);
                latch.countDown();
            }

            @Override public void failure(StackMobException e) {
                asserter.markTrue(e.getMessage().contains("Invalid"));
                latch.countDown();
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void logout() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();

        final User user = new User(username, password);
        final StackMobObjectOnServer<User> objectOnServer = createOnServer(user, User.class);

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", user.username);
        params.put("password", user.password);

        final CountDownLatch loginLatch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.login(params, new StackMobCallback() {
            @Override public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                final CountDownLatch logoutLatch = latchOne();

                stackmob.logout(new StackMobCallback() {
                    @Override public void success(String responseBody2) {
                        asserter.markNotNull(responseBody2);
                        asserter.markNotJsonError(responseBody2);
                        logoutLatch.countDown();
                    }
                    @Override public void failure(StackMobException e) {
                        asserter.markException(e);
                    }
                });

                try {
                    asserter.markLatchFinished(logoutLatch);
                    loginLatch.countDown();
                }
                catch(InterruptedException e) {
                    asserter.markFailure("logout did not complete");
                }
            }
            @Override public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });

        asserter.assertLatchFinished(loginLatch, CountDownLatchUtils.MAX_LATCH_WAIT_TIME);
        objectOnServer.delete();
    }

    @Test public void startSession() throws InterruptedException, StackMobException {
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.startSession(new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotNull(responseBody);
                asserter.markNotJsonError(responseBody);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void testTimeSync() throws Exception {
        //Hack a bad local time into the session
        StackMob.getStackMob().setSession(new StackMobSession(StackMob.getStackMob().getSession()) {
            @Override
            public long getLocalTime() {
                return super.getLocalTime() + 5000;
            }
        });
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        //This will fail, but it should cause us to sync up with the server
        stackmob.startSession(new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markException(new Exception("request with bad time succeeded"));
            }

            @Override
            public void failure(StackMobException e) {
                latch.countDown();
            }
        });
        asserter.assertLatchFinished(latch);
        //After startSession we should be accounting for the bad local time
        getWithoutArguments();
    }

    @Test public void getWithoutArguments() throws Exception {
        final Game game = new Game(Arrays.asList("one", "two"), "one");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(game, Game.class);
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.get("game", new StackMobCallback() {
            @Override public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markFalse(games.isEmpty());
                latch.countDown();
            }

            @Override public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        objectOnServer.delete();
    }

    @Test public void getWithArguments() throws Exception {
        final Game game = new Game(Arrays.asList("one", "two"), "one");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(game, Game.class);

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("name", game.name);
        stackmob.get("game", arguments, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                asserter.markEquals("one", games.get(0).name);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
                latch.countDown();
            }
        });
        asserter.assertLatchFinished(latch);

        objectOnServer.delete();
    }

    @Test
    public void getWithQuery() throws InterruptedException, StackMobException {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").fieldIsGreaterThanOrEqualTo("name", "sup");
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.get(query, new StackMobCallback() {
            @Override public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                asserter.markEquals("woot", games.get(0).name);
                latch.countDown();
            }
            @Override public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        objectOnServer.delete();
    }

    @Test public void getWithQueryWithField() throws Exception {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery q = new StackMobQuery("game");
        StackMobQueryWithField qWithField = new StackMobQueryWithField("name", q).isGreaterThanOrEqualTo("sup");
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.get(qWithField, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                asserter.markEquals("woot", games.get(0).name);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        objectOnServer.delete();
    }

    @Test public void getWithOrderBy() throws Exception {
        List<String> players = Arrays.asList("one", "two");
        final Game g1 = new Game(players, "gamea");
        final Game g2 = new Game(players, "gameb");
        final StackMobObjectOnServer<Game> g1OnServer = createOnServer(g1, Game.class);
        final StackMobObjectOnServer<Game> g2OnServer = createOnServer(g2, Game.class);

        StackMobQuery q = StackMobQuery.objects("game").field("name").isOrderedBy(StackMobQuery.Ordering.ASCENDING).getQuery();

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.get(q, new StackMobCallback() {
            @Override public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> gamesFromServer = gson.fromJson(responseBody, Game.ListTypeToken);
                Game prevGame = null;
                for(Game g: gamesFromServer) {
                    if(prevGame != null) {
                        asserter.markTrue(g.getName().compareTo(prevGame.getName()) >= 0);
                    }
                    prevGame = g;
                }
                latch.countDown();
            }

            @Override public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        g1OnServer.delete();
        g2OnServer.delete();
    }

    @Test public void getWithRange() throws Exception {
        List<String> players = Arrays.asList("one", "two");
        final Game g1 = new Game(players, "gamea");
        final Game g2 = new Game(players, "gameb");
        final Game g3 = new Game(players, "gamec");
        final StackMobObjectOnServer<Game> g1OnServer = createOnServer(g1, Game.class);
        final StackMobObjectOnServer<Game> g2OnServer = createOnServer(g2, Game.class);
        final StackMobObjectOnServer<Game> g3OnServer = createOnServer(g3, Game.class);

        final int rangeStart = 1;
        final int rangeEnd = 2;
        StackMobQuery q = StackMobQuery.objects("game").isInRange(rangeStart, rangeEnd);

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.get(q, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markEquals(rangeEnd - rangeStart + 1, games.size());
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });

        asserter.assertLatchFinished(latch);
        g1OnServer.delete();
        g2OnServer.delete();
        g3OnServer.delete();
    }

    @Test public void postWithRequestObject() throws Exception {
        final Game g = new Game(Arrays.asList("one", "two"), "newGame");
        g.name = "newGame";
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.post("game", g, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                Game game = gson.fromJson(responseBody, Game.class);
                StackMobObjectOnServer<Game> onServer = new StackMobObjectOnServer<Game>(stackmob, game.game_id, game);
                try {
                    onServer.delete();
                } catch (StackMobException e) {
                    asserter.markException(e);
                }

                asserter.markEquals("newGame", game.name);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }


   /*
    * This test requires manual setup to pass. Your user2 schema must have a field "photo" of type binary.
    * That should trigger it to upload the file to s3 rather than just storing the text
    */
    @Test public void postWithBinaryFile() throws Exception {
        final String contentType = "text/plain";
        final String fileName = "test.jpg";
        final String content = "w00t";
        final String schema = "user2";
        final String binaryField = "photo";
        final StackMobFile obj = new StackMobFile(contentType, fileName, content.getBytes());
        final String expectedAWSPrefix = "http://s3.amazonaws.com/test-stackmob/" + schema;

        Map<String, String> args = new HashMap<String, String>();
        args.put("username", "bob");
        args.put("photo", obj.toString());

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.post(schema, args, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                S3Object obj = gson.fromJson(responseBody, S3Object.class);
                StackMobObjectOnServer<S3Object> objOnServer = new StackMobObjectOnServer<S3Object>(stackmob, obj.user_id, obj);
                try {
                    objOnServer.delete();
                }
                catch (StackMobException e) {
                    asserter.markException(e);
                }
                asserter.markFalse(obj.photo.startsWith("Content-Type:"));
                asserter.markTrue(obj.photo.startsWith(expectedAWSPrefix));
                asserter.markTrue(obj.photo.endsWith(fileName));

                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void deleteWithId() throws Exception {
        final Game game = new Game(new ArrayList<String>(), "gameToDelete");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(game, Game.class);
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.delete("game", objectOnServer.getObjectId(), new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void put() throws Exception {
        final String oldName = "oldGameName";
        final String newName = "newGameName";

        final Game game = new Game(Arrays.asList("one", "two"), oldName);
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(game, Game.class);
        final String objectId = objectOnServer.getObjectId();

        game.name = newName;
        final Game updatedGame = new Game(Arrays.asList("modified", "modified2"), "modified_game");
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.put(game.getName(), objectId, updatedGame, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                Game jsonGame = gson.fromJson(responseBody, Game.class);
                asserter.markNotNull(jsonGame);
                asserter.markEquals(updatedGame.name, jsonGame.name);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        objectOnServer.delete();
        
    }
  
    @Test public void postBulk() throws Exception {
        final Game game1 = new Game(Arrays.asList("one", "two"), "game1");
        final Game game2 = new Game(Arrays.asList("one", "two"), "game2");
        
        List<Game> games = new ArrayList<Game>();
        games.add(game1);
        games.add(game2);
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        stackmob.postBulk(game1.getName(), games, new StackMobCallback() {
          @Override
          public void success(String responseBody) {
              asserter.markNotJsonError(responseBody);
              latch.countDown();
          }

          @Override
          public void failure(StackMobException e) {
              asserter.markException(e);
          }
        });
        asserter.assertLatchFinished(latch);
    }
  
    @Test public void postRelatedBulk() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();
        
        User user = new User(username, password);
        List<Object> users = new ArrayList<Object>();
        users.add(user);

        final Game game = new Game(new ArrayList<String>(), "gamepostrelatedbulk");
        final StackMobObjectOnServer<Game> gameOnServer = createOnServer(game, Game.class);

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        
        stackmob.postRelatedBulk(game.getName(), gameOnServer.getObjectId(), "moderators", users, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });

        asserter.assertLatchFinished(latch);
        gameOnServer.delete();
    }
    
    @Test public void postRelated() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();
  
        User user = new User(username, password);

        final Game game = new Game(new ArrayList<String>(), "gamepostrelated");
        final StackMobObjectOnServer<Game> gameOnServer = createOnServer(game, Game.class);
        
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        
        stackmob.postRelated(game.getName(), gameOnServer.getObjectId(), "moderators", user, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
              asserter.markNotJsonError(responseBody);
              latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
              asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        gameOnServer.delete();
    }
  
    @Test public void putRelated() throws Exception {
        
        final Game game = new Game(new ArrayList<String>(), "gameputrelated");
        final StackMobObjectOnServer<Game> gameOnServer = createOnServer(game, Game.class);
        
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
      
        List<String> newModerators = Arrays.asList("one","two");
        
        stackmob.putRelated(game.getName(), gameOnServer.getObjectId(), "moderators", newModerators, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                Game jsonGame = gson.fromJson(responseBody, Game.class);
                asserter.markTrue(jsonGame.moderators.contains("one") && jsonGame.moderators.contains("two"));
                latch.countDown();
            }
    
            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        gameOnServer.delete();
    }
  
    @Test public void deleteFromRelated() throws Exception {
        
        final Game game = new Game(new ArrayList<String>(), "gamedeleterelated");
        game.moderators = Arrays.asList("one","two","three");
        
        final StackMobObjectOnServer<Game> gameOnServer = createOnServer(game, Game.class);
        
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
      
        List<String> idsToDelete = Arrays.asList("two","three");
        stackmob.deleteIdsFrom(game.getName(), gameOnServer.getObjectId(), "moderators", idsToDelete, false, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                latch.countDown();
            }
    
            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        gameOnServer.delete();
    }

    @Test public void registerToken() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();
        final String token = getRandomString();

        User user = new User(username, password);
        final StackMobObjectOnServer<User> objectOnServer = createOnServer(user, User.class);
        final String objectId = objectOnServer.getObjectId();

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.registerForPushWithUser(objectId, token, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        objectOnServer.delete();
    }

    @Test public void getTokensForUsers() throws Exception {
        final String username = getRandomString();
        final List<String> usernames = new ArrayList<String>();
        usernames.add(username);
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getTokensForUsers(usernames, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void forgotPassword() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();
        final String email = getRandomString();
        final User user = new User(username, password, email);
        final StackMobObjectOnServer<User> objectOnServer = createOnServer(user, User.class);
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        stackmob.forgotPassword(username, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void loginAfterForgotPassword() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();
        final String email = getRandomString();
        final User user = new User(username, password, email);
        final StackMobObjectOnServer<User> objectOnServer = createOnServer(user, User.class);
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        stackmob.forgotPassword(username, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", user.username);
                params.put("password", user.password);
                params.put("email", user.email);
                stackmob.login(params, new StackMobCallback() {
                    @Override
                    public void success(String responseBody) {
                        asserter.markNotJsonError(responseBody);
                        latch.countDown();
                    }

                    @Override
                    public void failure(StackMobException e) {
                        asserter.markException(e);
                    }
                });
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void resetPassword() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();
        final String email = getRandomString();
        final User user = new User(username, password, email);
        final StackMobObjectOnServer<User> objectOnServer = createOnServer(user, User.class);
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", user.username);
        params.put("password", user.password);
        params.put("email", user.email);
        stackmob.login(params, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                stackmob.resetPassword(user.password, "foo", new StackMobCallback() {
                    @Override
                    public void success(String responseBody) {
                        asserter.markNotJsonError(responseBody);
                        latch.countDown();
                    }

                    @Override
                    public void failure(StackMobException e) {
                        asserter.markException(e);
                    }
                });
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }
}
