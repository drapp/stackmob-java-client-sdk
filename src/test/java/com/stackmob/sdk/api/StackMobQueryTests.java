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
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class StackMobQueryTests extends StackMobTestCommon {
    final String object = "user";
    final String field = "testField";
    final String otherField = "testField_Other";
    final String value = "testVal";
    final List<String> valueArr = Arrays.asList("one", "two", "three");
    final StackMobGeoPoint origin = new StackMobGeoPoint(0d, 0d);

    //should return ArrayList so remove will work (many List implementations will throw otherwise)
    private ArrayList<String> getExpectedRelationalKeys() {
        return new ArrayList<String>(Arrays.asList(
                                         field + StackMobQuery.Operator.LT.getOperatorForURL(),
                                         field + StackMobQuery.Operator.GT.getOperatorForURL(),
                                         field + StackMobQuery.Operator.LTE.getOperatorForURL(),
                                         field + StackMobQuery.Operator.GTE.getOperatorForURL()));
    }

    private void assertKeysAndValuesMatch(List<Map.Entry<String, String>> map, ArrayList<String> expectedKeys, String expectedValue) {
        assertEquals(expectedKeys.size(), map.size());
        List<String> keysClone = new ArrayList<String>(expectedKeys);
        for(Map.Entry<String, String> entry: map) {
            String key = entry.getKey();
            String val = entry.getValue();
            assertEquals(expectedValue, val);
            assertTrue(keysClone.remove(key));
        }
        assertEquals(0, keysClone.size());
    }

    private void assertKeysAndValuesMatch(List<Map.Entry<String, String>> map, List<String> expectedKeys, List<String> expectedValues) {
        assertEquals(expectedKeys.size(), expectedValues.size());
        assertEquals(expectedKeys.size(), map.size());
        for(int i = 0; i < map.size(); i++) {
            assertEquals(map.get(i).getKey(), expectedKeys.get(i));
            assertEquals(map.get(i).getValue(), expectedValues.get(i));
        }
    }

    @Test public void simpleStringQuery() {
        StackMobQuery q = new StackMobQuery(object)
                          .fieldIsLessThan(field, value)
                          .fieldIsGreaterThan(field, value)
                          .fieldIslessThanOrEqualTo(field, value)
                          .fieldIsGreaterThanOrEqualTo(field, value);

        assertEquals(object, q.getObjectName());
        List<Map.Entry<String, String>> args = q.getArguments();
        assertEquals(4, args.size());
        ArrayList<String> expectedKeys = getExpectedRelationalKeys();
        assertKeysAndValuesMatch(args, expectedKeys, value);
    }

    @Test public void inQuery() {
        StackMobQuery q = new StackMobQuery(object).fieldIsIn(field, valueArr);
        assertEquals(object, q.getObjectName());
        List<Map.Entry<String, String>> argList = q.getArguments();
        Map<String, String> args = new HashMap<String, String>();
        for(Map.Entry<String, String> entry : argList) {
            args.put(entry.getKey(), entry.getValue());
        }
        assertEquals(1, args.size());
        assertTrue(args.containsKey(field+StackMobQuery.Operator.IN.getOperatorForURL()));
        String val = args.get(field+StackMobQuery.Operator.IN.getOperatorForURL());
        List<String> valSplit = Arrays.asList(val.split(","));
        assertEquals(valueArr, valSplit);
    }

    @Test public void notEqualQuery() {
        StackMobQuery q = new StackMobQuery(object).fieldIsNotEqual(field, "foo");
        assertEquals(object, q.getObjectName());
        List<Map.Entry<String, String>> argList = q.getArguments();
        Map<String, String> args = new HashMap<String, String>();
        for(Map.Entry<String, String> entry : argList) {
            args.put(entry.getKey(), entry.getValue());
        }
        assertEquals(1, args.size());
        assertTrue(args.containsKey(field+StackMobQuery.Operator.NE.getOperatorForURL()));
        String val = args.get(field+StackMobQuery.Operator.NE.getOperatorForURL());
        List<String> valSplit = Arrays.asList(val.split(","));
        assertEquals(Arrays.asList("foo"), valSplit);
    }

    @Test public void isNullQuery() {
        StackMobQuery q = new StackMobQuery(object).fieldIsNull(field);
        assertEquals(object, q.getObjectName());
        List<Map.Entry<String, String>> argList = q.getArguments();
        Map<String, String> args = new HashMap<String, String>();
        for(Map.Entry<String, String> entry : argList) {
            args.put(entry.getKey(), entry.getValue());
        }
        assertEquals(1, args.size());
        assertTrue(args.containsKey(field+StackMobQuery.Operator.NULL.getOperatorForURL()));
        String val = args.get(field+StackMobQuery.Operator.NULL.getOperatorForURL());
        List<String> valSplit = Arrays.asList(val.split(","));
        assertEquals(Arrays.asList("true"), valSplit);
    }

    @Test public void isNotNullQuery() {
        StackMobQuery q = new StackMobQuery(object).fieldIsNotNull(field);
        assertEquals(object, q.getObjectName());
        List<Map.Entry<String, String>> argList = q.getArguments();
        Map<String, String> args = new HashMap<String, String>();
        for(Map.Entry<String, String> entry : argList) {
            args.put(entry.getKey(), entry.getValue());
        }
        assertEquals(1, args.size());
        assertTrue(args.containsKey(field+StackMobQuery.Operator.NULL.getOperatorForURL()));
        String val = args.get(field+StackMobQuery.Operator.NULL.getOperatorForURL());
        List<String> valSplit = Arrays.asList(val.split(","));
        assertEquals(Arrays.asList("false"), valSplit);
    }

    @Test public void nearQuery() {
        StackMobQuery q = new StackMobQuery(object).fieldIsNearWithinMi(field, origin, 1d);
        assertEquals(object, q.getObjectName());
        List<Map.Entry<String, String>> argList = q.getArguments();
        Map<String, String> args = new HashMap<String, String>();
        for(Map.Entry<String, String> entry : argList) {
            args.put(entry.getKey(), entry.getValue());
        }
        assertEquals(1, args.size());
        assertTrue(args.containsKey(field + StackMobQuery.Operator.NEAR.getOperatorForURL()));
        String val = args.get(field + StackMobQuery.Operator.NEAR.getOperatorForURL());
        List<String> valSplit = Arrays.asList(val.split(","));
        assertEquals(3, valSplit.size());
    }

    @Test public void withinRadiusQuery() {
        StackMobQuery q = new StackMobQuery(object).fieldIsWithinRadiusInMi(field, origin, 1d);
        assertEquals(object, q.getObjectName());
        List<Map.Entry<String, String>> argList = q.getArguments();
        Map<String, String> args = new HashMap<String, String>();
        for(Map.Entry<String, String> entry : argList) {
            args.put(entry.getKey(), entry.getValue());
        }
        assertEquals(1, args.size());
        assertTrue(args.containsKey(field+StackMobQuery.Operator.WITHIN.getOperatorForURL()));
        String val = args.get(field+StackMobQuery.Operator.WITHIN.getOperatorForURL());
        List<String> valSplit = Arrays.asList(val.split(","));
        assertEquals(3, valSplit.size());
    }

    @Test public void withinBoxQuery() {
        StackMobQuery q = new StackMobQuery(object).fieldIsWithinBox(field, origin, new StackMobGeoPoint(1d, 1d));
        assertEquals(object, q.getObjectName());
        List<Map.Entry<String, String>> argList = q.getArguments();
        Map<String, String> args = new HashMap<String, String>();
        for(Map.Entry<String, String> entry : argList) {
            args.put(entry.getKey(), entry.getValue());
        }
        assertEquals(1, args.size());
        assertTrue(args.containsKey(field+StackMobQuery.Operator.WITHIN.getOperatorForURL()));
        String val = args.get(field+StackMobQuery.Operator.WITHIN.getOperatorForURL());
        List<String> valSplit = Arrays.asList(val.split(","));
        List<String> expected = Arrays.asList("0.0", "0.0", "1.0", "1.0");
        assertEquals(expected, valSplit);
    }

    @Test public void multiFieldQueries() {
        StackMobQuery q = new StackMobQuery(object).field(new StackMobQueryField(field).isLessThan(value)).field(new StackMobQueryField(otherField).isGreaterThan(value));
        ArrayList<String> expectedKeys = new ArrayList<String>(Arrays.asList(
                field+StackMobQuery.Operator.LT.getOperatorForURL(),
                otherField+StackMobQuery.Operator.GT.getOperatorForURL()));
        List<Map.Entry<String, String>> args = q.getArguments();
        assertKeysAndValuesMatch(args, expectedKeys, value);
    }

    @Test public void simpleOr() {
        StackMobQuery q = new StackMobQuery(object).fieldIsEqualTo("dog", "herc").or().fieldIsEqualTo("cat", "fluffy");
        List<String> expectedKeys = Arrays.asList("[or1].cat", "[or1].dog");
        List<String> expectedValues = Arrays.asList("fluffy", "herc");
        List<Map.Entry<String, String>> args = q.getArguments();
        assertKeysAndValuesMatch(args, expectedKeys, expectedValues);
    }

    @Test public void nestedOr() {
        StackMobQuery q = new StackMobQuery(object).fieldIsEqualTo("dog", "herc").or(new StackMobQuery().fieldIsEqualTo("cat", "fluffy")
                                                                                                  .and().fieldIsEqualTo("color", "grey"));
        List<String> expectedKeys = Arrays.asList("[or1].[and1].color", "[or1].[and1].cat", "[or1].dog");
        List<String> expectedValues = Arrays.asList("grey", "fluffy", "herc");
        List<Map.Entry<String, String>> args = q.getArguments();
        assertKeysAndValuesMatch(args, expectedKeys, expectedValues);
    }
    @Test public void complexOr() {
        StackMobQuery q = new StackMobQuery(object).fieldIsGreaterThanOrEqualTo("age", 2)
                                                   .and(new StackMobQuery().fieldIsEqualTo("dog", "herc")
                                                                          .or(new StackMobQuery().fieldIsEqualTo("cat", "fluffy")
                                                                                           .and().fieldIsEqualTo("color", "grey"))
                                                                          .or(new StackMobQuery().fieldIsEqualTo("dog", "bodie")
                                                                                           .and().fieldIsEqualTo("bodiness", 500)))
                                                   .and(new StackMobQuery().fieldIsEqualTo("numberoflegs", 4)
                                                                      .or().fieldIsEqualTo("numberofwings", 2));
        List<String> expectedKeys = Arrays.asList("[or1].[and1].color", "[or2].numberoflegs", "[or1].[and2].dog", "[or1].[and1].cat", "[or1].[and2].bodiness", "age[gte]", "[or2].numberofwings", "[or1].dog");
        List<String> expectedValues = Arrays.asList("grey", "4", "bodie", "fluffy", "500", "2", "2", "herc");
        List<Map.Entry<String, String>> args = q.getArguments();
        assertKeysAndValuesMatch(args, expectedKeys, expectedValues);
    }
}
