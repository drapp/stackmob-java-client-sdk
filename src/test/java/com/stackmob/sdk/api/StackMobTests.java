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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.stackmob.sdk.StackMobTestCommon;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.callback.StackMobCountCallback;
import com.stackmob.sdk.concurrencyutils.CountDownLatchUtils;
import com.stackmob.sdk.concurrencyutils.MultiThreadAsserter;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.push.StackMobPush;
import com.stackmob.sdk.push.StackMobPushToken;
import com.stackmob.sdk.testobjects.*;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static com.stackmob.sdk.concurrencyutils.CountDownLatchUtils.latchOne;

@SuppressWarnings("deprecation")
public class StackMobTests extends StackMobTestCommon {

    @Test public void loginShouldFail() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("password", password);

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.login(params, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markJsonError(responseBody);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markTrue(e.getMessage().contains("Invalid"));
                latch.countDown();
            }
        });
        asserter.assertLatchFinished(latch);
    }

    public StackMobObjectOnServer<UserOnServer> doLoginLogout(StackMob.OAuthVersion version, final boolean logout) throws Exception {
        StackMob.getStackMob().getSession().setOAuthVersion(version);
        final String username = getRandomString();
        final String password = getRandomString();

        final UserOnServer user = new UserOnServer(username, password);
        final StackMobObjectOnServer<UserOnServer> objectOnServer = createOnServer(user, UserOnServer.class);

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", user.username);
        params.put("password", user.password);

        final CountDownLatch loginLatch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        asserter.markTrue(!stackmob.isLoggedIn());
        asserter.markTrue(!stackmob.isLoggedOut());
        asserter.markTrue(stackmob.getLoggedInUsername() == null);
        asserter.markTrue(!stackmob.isUserLoggedIn(user.username));

        stackmob.login(params, new StackMobCallback() {
            @Override public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                final CountDownLatch logoutLatch = latchOne();

                asserter.markTrue(stackmob.isLoggedIn());
                asserter.markTrue(!stackmob.isLoggedOut());
                asserter.markTrue(stackmob.getLoggedInUsername().equals(user.username));
                asserter.markTrue(stackmob.isUserLoggedIn(user.username));

                if(logout) {
                    stackmob.logout(new StackMobCallback() {
                        @Override public void success(String responseBody2) {
                            asserter.markNotNull(responseBody2);
                            asserter.markTrue(!stackmob.isLoggedIn());
                            asserter.markTrue(stackmob.isLoggedOut());
                            asserter.markTrue(stackmob.getLoggedInUsername() == null);
                            asserter.markTrue(!stackmob.isUserLoggedIn(user.username));
                            asserter.markNotJsonError(responseBody2);
                            logoutLatch.countDown();
                        }
                        @Override public void failure(StackMobException e) {
                            asserter.markException(e);
                        }
                    });
                }

                try {
                    if(logout) asserter.markLatchFinished(logoutLatch);
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

        // We aren't actually logged in yet here
        asserter.markTrue(!stackmob.isLoggedIn());
        asserter.markTrue(!stackmob.isLoggedOut());
        asserter.markTrue(stackmob.getLoggedInUsername() == null);
        asserter.markTrue(!stackmob.isUserLoggedIn(user.username));

        asserter.assertLatchFinished(loginLatch, CountDownLatchUtils.MAX_LATCH_WAIT_TIME);
        if(logout) objectOnServer.delete();
        return objectOnServer;
    }

    @Test public void loginLogout() throws Exception {
        doLoginLogout(StackMob.OAuthVersion.One, true);

    }

    @Test public void oauth2LoginShouldFail() throws Exception {
        StackMob.getStackMob().getSession().setOAuthVersion(StackMob.OAuthVersion.Two);
        final String username = getRandomString();
        final String password = getRandomString();

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("password", password);

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.login(params, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markJsonError(responseBody);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markTrue(e.getMessage().contains("access_denied"));
                latch.countDown();
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void oauth2LoginLogout() throws Exception {
        StackMobObjectOnServer<UserOnServer> user = doLoginLogout(StackMob.OAuthVersion.Two, false);
        final CountDownLatch localLatch = latchOne();
        final MultiThreadAsserter localAsserter = new MultiThreadAsserter();
        StackMob.getStackMob().getDatastore().get("restricted", new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                localLatch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                localAsserter.markException(e);
            }
        });
        localAsserter.assertLatchFinished(localLatch);
        user.delete();
    }

    @Test public void oauth2RefreshToken() throws Exception {
        StackMobObjectOnServer<UserOnServer> user = doLoginLogout(StackMob.OAuthVersion.Two, false);
        final CountDownLatch localLatch = latchOne();
        final MultiThreadAsserter localAsserter = new MultiThreadAsserter();
        StackMob.getStackMob().refreshLogin(new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                localLatch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                localAsserter.markException(e);
            }
        });
        localAsserter.assertLatchFinished(localLatch);
        user.delete();
    }

    @Ignore @Test public void testTimeSync() throws Exception {
        //Hack a bad local time into the session
        StackMob.getStackMob().setSession(new StackMobSession(StackMob.getStackMob().getSession()) {
            @Override
            public long getLocalTime() {
                StackMob.getStackMob().getSession().getLogger().logWarning("Mocking incorrect time");
                return super.getLocalTime() + 5000;
            }
        });
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        //This will fail, but it should cause us to sync up with the server
        StackMob.getStackMob().getLoggedInUser(new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markException(new Exception("request with bad time succeeded"));
                latch.countDown();
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

        stackmob.getDatastore().get("game", new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markFalse(games.isEmpty());
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

    @Test public void getWithExpand() throws Exception {

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        stackmob.getDatastore().get("account", StackMobOptions.depthOf(1), new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                JsonElement elt = new JsonParser().parse(responseBody);
                asserter.markNotNull(elt.getAsJsonArray().get(1).getAsJsonObject().get("business").getAsJsonObject().get("business_id"));
                asserter.markNotJsonError(responseBody);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
                latch.countDown();
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void getWithQuery() throws InterruptedException, StackMobException {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").fieldIsGreaterThanOrEqualTo("name", "sup");
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(query, new StackMobCallback() {
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

    @Test public void getWithPagination() throws InterruptedException, StackMobException {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").fieldIsGreaterThanOrEqualTo("name", "sup").isInRange(0, 9);
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(query, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() == 10);
                asserter.markTrue(getTotalObjectCountFromPagination() > 10);
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

    @Test public void getWithInQuery() throws InterruptedException, StackMobException {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").fieldIsIn("name", Arrays.asList("woot"));
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(query, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markTrue("woot".equals(g.name));
                }
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

    @Test public void getWithInQueryWithField() throws InterruptedException, StackMobException {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").field(new StackMobQueryField("name").isIn(Arrays.asList("woot")));
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(query, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markTrue("woot".equals(g.name));
                }
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

    @Test public void getWithNotInQuery() throws InterruptedException, StackMobException {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").fieldIsNotIn("name", Arrays.asList("boot"));
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(query, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markFalse("boot".equals(g.name));
                }
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

    @Test public void getWithNotInQueryWithField() throws InterruptedException, StackMobException {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").field(new StackMobQueryField("name").isNotIn(Arrays.asList("boot")));
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(query, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markFalse("boot".equals(g.name));
                }
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

    @Test public void getWithNotEqualQuery() throws InterruptedException, StackMobException {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").fieldIsNotEqual("name", "woot");
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(query, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markFalse("woot".equals(g.name));
                }
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

    @Test public void getWithNotEqualQueryWithField() throws Exception {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery q = new StackMobQuery("game").field(new StackMobQueryField("name").isNotEqualTo("woot"));
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(q, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markFalse("woot".equals(g.name));
                }
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

    @Test public void getWithIsNotNullQuery() throws InterruptedException, StackMobException {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").fieldIsNotNull("players");
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(query, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markNotNull(g.players);
                }
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

    @Test public void getWithIsNotNullQueryWithField() throws Exception {
        final Game g = new Game(Arrays.asList("seven", "six"), "woot");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery q = new StackMobQuery("game").field(new StackMobQueryField("players").isNotNull());
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(q, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markNotNull(g.players);
                }
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

    @Test public void getWithIsNullQuery() throws InterruptedException, StackMobException {
        final Game g = new Game(null, "woot1");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery query = new StackMobQuery("game").fieldIsNull("players");
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(query, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markTrue(g.players == null);
                }
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

    @Test public void getWithIsNullQueryWithField() throws Exception {
        final Game g = new Game(null, "woot1");
        final StackMobObjectOnServer<Game> objectOnServer = createOnServer(g, Game.class);

        StackMobQuery q = new StackMobQuery("game").field(new StackMobQueryField("players").isNull());
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(q, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> games = gson.fromJson(responseBody, Game.ListTypeToken);
                asserter.markNotNull(games);
                asserter.markTrue(games.size() >= 1);
                for (Game g : games) {
                    asserter.markTrue(g.players == null);
                }
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

        StackMobQuery q = StackMobQuery.objects("game").field(new StackMobQueryField("name").isOrderedBy(StackMobQuery.Ordering.ASCENDING));

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().get(q, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                List<Game> gamesFromServer = gson.fromJson(responseBody, Game.ListTypeToken);
                Game prevGame = null;
                for (Game g : gamesFromServer) {
                    if (prevGame != null) {
                        asserter.markTrue(g.getName().compareTo(prevGame.getName()) >= 0);
                    }
                    prevGame = g;
                }
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

        stackmob.getDatastore().get(q, new StackMobCallback() {
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

    public void doPostWithRequestObject(StackMob localStackmob) throws Exception {
        final Game g = new Game(Arrays.asList("one", "two"), "newGame");
        g.name = "newGame";
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        localStackmob.getDatastore().post("game", g, new StackMobCallback() {
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

    @Test public void postWithRequestObject() throws Exception {
        doPostWithRequestObject(StackMob.getStackMob());
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
        final StackMobFile obj = new StackMobFile(contentType, fileName, content.getBytes());
        final String expectedAWSPrefix = "http://s3.amazonaws.com/test-stackmob/" + schema;

        Map<String, String> args = new HashMap<String, String>();
        args.put("username", "bob");
        args.put("photo", obj.toString());

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().post(schema, args, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                S3Object obj = gson.fromJson(responseBody, S3Object.class);
                StackMobObjectOnServer<S3Object> objOnServer = new StackMobObjectOnServer<S3Object>(stackmob, obj.user_id, obj);
                try {
                    objOnServer.delete();
                } catch (StackMobException e) {
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

        stackmob.getDatastore().delete("game", objectOnServer.getObjectId(), new StackMobCallback() {
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

    @Test public void deleteByQuery() throws Exception {
        final String gameOneId = "gameByQueryOne";
        final String gameTwoId = "gameByQueryTwo";
        final Game gameOne = new Game(new ArrayList<String>(), gameOneId);
        final Game gameTwo = new Game(new ArrayList<String>(), gameTwoId);
        final StackMobObjectOnServer<Game> objectOnServerOne = createOnServer(gameOne, Game.class);
        final StackMobObjectOnServer<Game> objectOnServerTwo = createOnServer(gameTwo, Game.class);
        StackMobQuery query = new StackMobQuery("game").field(new StackMobQueryField("name").isIn(Arrays.asList(gameOneId, gameTwoId)));
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().delete(query, new StackMobCallback() {
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

        stackmob.getDatastore().put(game.getName(), objectId, updatedGame, new StackMobCallback() {
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
        stackmob.getDatastore().postBulk(game1.getName(), games, new StackMobCallback() {
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
        
        UserOnServer user = new UserOnServer(username, password);
        List<Object> users = new ArrayList<Object>();
        users.add(user);

        final Game game = new Game(new ArrayList<String>(), "gamepostrelatedbulk");
        final StackMobObjectOnServer<Game> gameOnServer = createOnServer(game, Game.class);

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        
        stackmob.getDatastore().postRelatedBulk(game.getName(), gameOnServer.getObjectId(), "moderators", users, new StackMobCallback() {
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
  
        UserOnServer user = new UserOnServer(username, password);

        final Game game = new Game(new ArrayList<String>(), "gamepostrelated");
        final StackMobObjectOnServer<Game> gameOnServer = createOnServer(game, Game.class);
        
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        
        stackmob.getDatastore().postRelated(game.getName(), gameOnServer.getObjectId(), "moderators", user, new StackMobCallback() {
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
        
        stackmob.getDatastore().putRelated(game.getName(), gameOnServer.getObjectId(), "moderators", newModerators, new StackMobCallback() {
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
        stackmob.getDatastore().deleteIdsFrom(game.getName(), gameOnServer.getObjectId(), "moderators", idsToDelete, false, new StackMobCallback() {
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

        UserOnServer user = new UserOnServer(username, password);
        final StackMobObjectOnServer<UserOnServer> objectOnServer = createOnServer(user, UserOnServer.class);
        final String objectId = objectOnServer.getObjectId();

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        StackMobPush.getPush().registerForPushWithUser(new StackMobPushToken(token), objectId, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);
                pushToToken(token, latch, asserter);
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        objectOnServer.delete();
    }

    @Test public void registeriOSToken() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();

        UserOnServer user = new UserOnServer(username, password);
        final StackMobObjectOnServer<UserOnServer> objectOnServer = createOnServer(user, UserOnServer.class);

        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();


        final User bodie = new User("bodie");
        final StackMobPushToken pushToken = new StackMobPushToken("0000000000000000000000000000000000000000000000000000000000000000", StackMobPushToken.TokenType.iOS);
        bodie.registerForPush(pushToken, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotJsonError(responseBody);

                bodie.removeFromPush(pushToken, new StackMobCallback() {
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
        objectOnServer.delete();
    }

    public void pushToToken(String token, final CountDownLatch latch, final MultiThreadAsserter asserter) {
        StackMobPushToken t = new StackMobPushToken(token, StackMobPushToken.TokenType.Android);
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("foo", "bar");
        StackMobPush.getPush().pushToTokens(payload, Arrays.asList(t), new StackMobCallback() {
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

    @Test public void getTokensForUsers() throws Exception {
        final String username = getRandomString();
        final List<String> usernames = new ArrayList<String>();
        usernames.add(username);
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        StackMobPush.getPush().getTokensForUsers(usernames, new StackMobCallback() {
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
        final UserOnServer user = new UserOnServer(username, password, email);
        createOnServer(user, UserOnServer.class);
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
        final UserOnServer user = new UserOnServer(username, password, email);
        createOnServer(user, UserOnServer.class);
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

    public void doResetPassword() throws Exception {
        final String username = getRandomString();
        final String password = getRandomString();
        final String email = getRandomString();
        final UserOnServer user = new UserOnServer(username, password, email);
        createOnServer(user, UserOnServer.class);
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

    @Test public void resetPassword() throws Exception {
        doResetPassword();
    }
    
    private void testCount(StackMobQuery query, final int expectedCount) throws Exception {
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        stackmob.getDatastore().count(query, new StackMobCountCallback() {
            @Override
            public void success(long result) {
                asserter.markEquals((int) result, expectedCount);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }
    
    @Test
    public void countZero() throws Exception {
        testCount(new StackMobQuery("justzero"), 0);
    }

    @Test
    public void countOne() throws Exception {
        testCount(new StackMobQuery("justone"), 1);
    }

    @Test
    public void countMany() throws Exception {
        testCount(new StackMobQuery("justmany"), 6);
    }

    @Test
    public void countQuery() throws Exception {
        testCount(new StackMobQuery("justmany").fieldIsGreaterThan("foo", "foo"), 2);
    }

    static class CounterTest extends StackMobObject {
        String countertest_id;

        public CounterTest(String id, int i) {
            countertest_id = id;
            counter = i;
        }
        int counter;

        @Override
        public String getIdField() {
            return countertest_id;
        }

        @Override
        public String getIdFieldName() {
            return "countertest_id";
        }

        @Override
        public String getName() {
            return "countertest";
        }
    }

    @Test
    public void testUpdateAtomicCounter() throws Exception {
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        CounterTest test = new CounterTest("foo", 5);
        StackMobObjectOnServer<CounterTest> obj = createOnServer(test, CounterTest.class);
        StackMob.getStackMob().getDatastore().updateAtomicCounter("countertest", obj.getObject().getIdField(), "counter", -2, new StackMobCallback() {
            @Override public void success(String result) {
                asserter.markEquals(3, new JsonParser().parse(result).getAsJsonObject().get("counter").getAsJsonPrimitive().getAsInt());
                latch.countDown();
            }
            @Override public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        obj.delete();
    }

    @Test
    public void testPutWithUpdateAtomicCounter() throws Exception {
        final CountDownLatch latch = latchOne();
        final MultiThreadAsserter asserter = new MultiThreadAsserter();
        CounterTest test = new CounterTest("foo", 5);
        StackMobObjectOnServer<CounterTest> obj = createOnServer(test, CounterTest.class);
        test.counter = -2;
        StackMob.getStackMob().getDatastore().putAndUpdateAtomicCounters("countertest", "foo", test, Arrays.asList("counter"), new StackMobCallback() {
            @Override public void success(String result) {
                asserter.markEquals(3, new JsonParser().parse(result).getAsJsonObject().get("counter").getAsJsonPrimitive().getAsInt());
                latch.countDown();
            }
            @Override public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
        obj.delete();
    }
}
