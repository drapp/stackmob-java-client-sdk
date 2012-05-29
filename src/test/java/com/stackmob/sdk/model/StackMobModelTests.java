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
import com.stackmob.sdk.StackMobTestCommon;
import com.stackmob.sdk.api.StackMob;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.concurrencyutils.MultiThreadAsserter;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.testobjects.Author;
import com.stackmob.sdk.testobjects.Book;
import com.stackmob.sdk.testobjects.Library;
import com.stackmob.sdk.util.RelationMapping;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

import static com.stackmob.sdk.concurrencyutils.CountDownLatchUtils.latch;
import static com.stackmob.sdk.concurrencyutils.CountDownLatchUtils.latchOne;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class StackMobModelTests extends StackMobTestCommon {
    
    /* Offline */

    private static class Simple extends StackMobModel {
        public Simple() {
            super(Simple.class);
        }
        public Simple(Class<? extends StackMobModel> actualClass) {
            super(actualClass);
        }
        
        public Simple(String id) {
            this();
            setID(id);
        }
        public Simple(String id, String foo, int bar) {
            this(id);
            this.foo = foo;
            this.bar = bar;
        }
        protected String foo = "test";
        protected int bar = 5;
    }
    
    @Test public void testBasicBehavior() throws Exception {
        Simple simple = new Simple();
        assertEquals("simple", simple.getSchemaName());
        assertEquals("simple_id", simple.getIDFieldName());
        RelationMapping mapping = new RelationMapping();
        JsonObject obj = new JsonParser().parse(simple.toJsonWithDepth(0, mapping)).getAsJsonObject();
        assertEquals("test", obj.get("foo").getAsString());
        assertEquals(5, obj.get("bar").getAsInt());
        assertNotNull(obj.get("simple_id").getAsString());
        assertEquals("",mapping.toHeaderString());
    }

    private class Complicated extends Simple {
        public Complicated() {
            super(Complicated.class);
        }
        private long number = 1337;
        private UUID uuid = new UUID(3,4);
        private String[] strings = new String[] {"hello", "world"};
        private boolean test = false;
        private byte[] myBytes = new byte[] {(byte)0xaf, (byte)0x45, (byte)0xf3};
        protected Date date = new Date();
    }
    
    @Test public void testComplicatedTypes() throws Exception {
        RelationMapping mapping = new RelationMapping();
        String json = new Complicated().toJsonWithDepth(0, mapping);
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        assertTrue(object.get("foo").getAsJsonPrimitive().isString());
        assertTrue(object.get("bar").getAsJsonPrimitive().isNumber());
        assertTrue(object.get("number").getAsJsonPrimitive().isNumber());
        assertTrue(object.get("uuid").getAsJsonPrimitive().isString());
        assertTrue(object.get("strings").isJsonArray() && object.get("strings").getAsJsonArray().iterator().next().getAsJsonPrimitive().isString());
        assertTrue(object.get("test").getAsJsonPrimitive().isBoolean());
        assertTrue(object.get("mybytes").isJsonArray() && object.get("mybytes").getAsJsonArray().iterator().next().getAsJsonPrimitive().isNumber());
        assertTrue(object.get("date").getAsJsonPrimitive().isNumber());
        assertEquals("",mapping.toHeaderString());
    }

    private class Subobject extends StackMobModel {
        private CountDownLatch Latch = latchOne();
        public Subobject() {
            super(Subobject.class);
        }
    }
    
    @Test public void testSubobject() throws Exception {
        try {
            new Subobject().toJsonWithDepth(0, new RelationMapping());
            assertTrue(false);
        } catch(Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }

    String bookName1 = "The C Programming Language";
    String bookPublisher1 = "Prentice Hall";
    
    @Test public void testFillUnexpandedJSON() throws Exception {
        String json = "{\"title\":\"" + bookName1 + "\"," +
                   "\"publisher\":\"" + bookPublisher1 +"\"," +
                       "\"author\":KnR}";
        System.out.println(json);
        Book book = new Book();
        book.fillFromJson(new JsonParser().parse(json));
        assertEquals(bookName1, book.getTitle());
        assertEquals(bookPublisher1, book.getPublisher());
        assertNotNull(book.getAuthor());
        assertEquals("KnR", book.getAuthor().getID());
        assertNull(book.getAuthor().getName());

    }

    @Test public void testFillExpandedJSON() throws Exception {
        String json = "{\"title\":\"" + bookName1 + "\"," +
                   "\"publisher\":\"" + bookPublisher1 +"\", " +
                       "\"author\":{\"author_id\":\"KnR\", " +
                                     "\"name\":\"Kernighan and Ritchie\"}}";
        Book book = new Book();
        book.fillFromJson(new JsonParser().parse(json));
        assertEquals(bookName1, book.getTitle());
        assertEquals(bookPublisher1, book.getPublisher());
        assertNotNull(book.getAuthor());
        assertEquals("KnR", book.getAuthor().getID());
        assertEquals("Kernighan and Ritchie", book.getAuthor().getName());
    }
    
    @Test public void testFillComplicatedJSON() throws Exception {
        String json = "{\"number\":1338,\"strings\":[\"hello!\",\"world!\"],\"test\":true,\"mybytes\":[1,2,3],\"foo\":\"testpassed\",\"bar\":27,\"uuid\":\"00000000-0000-0003-0000-000000000005\",\"date\":0}";
        Complicated c = new Complicated();
        c.fillFromJson(new JsonParser().parse(json));
        assertEquals(c.foo,"testpassed");
        assertEquals(c.bar, 27);
        assertEquals(c.number, 1338);
        assertNotNull(c.uuid);
        assertEquals(c.uuid.toString(), "00000000-0000-0003-0000-000000000005");
        assertNotNull(c.strings);
        assertEquals(c.strings[0], "hello!");
        assertEquals(c.test,true);
        assertNotNull(c.myBytes);
        assertEquals(c.myBytes[0], 1);
        assertEquals(new Date(0), c.date);
    }
    
    private class Bad_Schema_Name extends StackMobModel {
        public Bad_Schema_Name() {
            super(Bad_Schema_Name.class);
        }    
        String foo = "fail";
    }

    @Test public void testBadSchemaName() throws Exception {
        try {
            new Bad_Schema_Name().toJsonWithDepth(0, new RelationMapping());
            assertTrue(false);
        } catch(Exception e) { }
    }

    private class BadFieldName extends StackMobModel {
        public BadFieldName() {
            super(BadFieldName.class);
        }
        String badfieldname_id = "fail";
    }

    @Test public void testBadFieldName() throws Exception {
        try {
            new BadFieldName().toJsonWithDepth(0, new RelationMapping());
            assertTrue(false);
        } catch(Exception e) { }
    }
    
    @Test public void testNestedModels() throws Exception {
        Author a = new Author("Terry Pratchett");
        a.setID("pratchett");
        Book b = new Book("Mort", "Harper Collins", a);
        RelationMapping mapping = new RelationMapping();
        String json = b.toJsonWithDepth(0, mapping);
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        assertTrue(object.get("title").getAsJsonPrimitive().getAsString().equals("Mort"));
        assertTrue(object.get("publisher").getAsJsonPrimitive().getAsString().equals("Harper Collins"));
        assertTrue(object.get("author").getAsJsonPrimitive().getAsString().equals("pratchett"));
        assertEquals("author=author", mapping.toHeaderString());
    }
    
    @Test public void testModelArrayToJSON() throws Exception {
        Library lib = new Library();
        lib.name = "SF Public Library";
        Author a = new Author("baz");
        a.setID("baz");
        Book b1 = new Book("foo","bar", a);
        b1.setID("foobar");
        Book b2 = new Book("foo2", "bar2", a);
        b2.setID("foo2bar2");
        lib.books = new Book[] {b1, b2};
        RelationMapping mapping = new RelationMapping();
        JsonElement json = new JsonParser().parse(lib.toJsonWithDepth(2, mapping));
        assertNotNull(json);
        assertTrue(json.isJsonObject());
        JsonObject jsonObject = json.getAsJsonObject();
        assertEquals(jsonObject.get("name").getAsString(), "SF Public Library");
        assertTrue(jsonObject.get("books").isJsonArray());
        JsonObject book1 = jsonObject.get("books").getAsJsonArray().get(0).getAsJsonObject();
        assertEquals("foobar", book1.get("book_id").getAsString());
        assertEquals("foo", book1.get("title").getAsString());
        assertEquals("bar", book1.get("publisher").getAsString());
        assertNotNull(book1.get("author"));
        JsonObject author1 = book1.get("author").getAsJsonObject();
        assertEquals("baz", author1.get("author_id").getAsString());
        assertEquals("baz", author1.get("name").getAsString());
        JsonObject book2 = jsonObject.get("books").getAsJsonArray().get(1).getAsJsonObject();
        assertEquals("foo2bar2", book2.get("book_id").getAsString());
        assertEquals("foo2", book2.get("title").getAsString());
        assertEquals("bar2", book2.get("publisher").getAsString());
        assertNotNull(book2.get("author"));
        JsonObject author2 = book2.get("author").getAsJsonObject();
        assertEquals("baz", author2.get("author_id").getAsString());
        assertEquals("baz", author2.get("name").getAsString());
        assertEquals("books=book&books.author=author",mapping.toHeaderString());

    }
    
    @Test public void testModelArrayFromJSON() throws Exception {
        String json = "{\"name\":\"SF Public Library\",\"books\":[{\"title\":\"foo\",\"publisher\":\"bar\",\"author\":{\"name\":\"baz\",\"author_id\":\"baz\"},\"book_id\":\"foobar\"},{\"title\":\"foo2\",\"publisher\":\"bar2\",\"author\":{\"name\":\"baz\",\"author_id\":\"baz\"},\"book_id\":\"foo2bar2\"}]}";
        Library lib = new Library();
        lib.fillFromJson(new JsonParser().parse(json));

        assertEquals(lib.name,"SF Public Library");
        assertNotNull(lib.books);
        assertNotNull(lib.books[0]);
        assertEquals(lib.books[0].getID(), "foobar");
        assertEquals(lib.books[0].getTitle(), "foo");
        assertEquals(lib.books[0].getPublisher(), "bar");
        assertNotNull(lib.books[0].getAuthor());
        assertEquals(lib.books[0].getAuthor().getID(), "baz");
        assertEquals(lib.books[0].getAuthor().getName(), "baz");
        assertNotNull(lib.books[1]);
        assertEquals(lib.books[1].getID(), "foo2bar2");
        assertEquals(lib.books[1].getTitle(), "foo2");
        assertEquals(lib.books[1].getPublisher(), "bar2");
        assertNotNull(lib.books[1].getAuthor());
        assertEquals(lib.books[1].getAuthor().getID(), "baz");
        assertEquals(lib.books[1].getAuthor().getName(), "baz");
    }
    
    @Test public void noIDChildrenToJSON() throws Exception {
        Book b = new Book("Oliver","Penguin",new Author("Dickens"));
        JsonElement json = new JsonParser().parse(b.toJsonWithDepth(1, new RelationMapping()));
        JsonObject authorObject =  json.getAsJsonObject().get("author").getAsJsonObject();
        assertEquals("Dickens",authorObject.get("name").getAsString());
        assertNotNull(authorObject.get("author_id"));
    }
    
    @Test public void noIDChildrenFromJSON() throws Exception {
        String json = "{\"title\":\"Oliver\",\"publisher\":\"Penguin\",\"author\":{\"name\":\"Dickens\"}}";
        Book b = new Book();
        b.fillFromJson(new JsonParser().parse(json));
        assertNull(b.getID());
        assertEquals("Oliver", b.getTitle());
        assertEquals("Penguin", b.getPublisher());
        assertNull(b.getAuthor().getID());
        assertEquals("Dickens", b.getAuthor().getName());
    }
    
    @Test public void testNotOverwritingExistingData() throws Exception {
        Book b = new Book("foo","bar", new Author("baz"));
        b.getAuthor().setID("baz");
        //The json has the same author with no data
        b.fillFromJson(new JsonParser().parse("{\"title\":\"foo\",\"publisher\":\"bar\",\"author\":\"baz\",\"book_id\":\"foobar\"}"));
        assertEquals("baz", b.getAuthor().getName());
    }

    @Test public void testHasSameID() {
        Simple simple = new Simple();
        assertFalse(simple.hasSameID(new JsonPrimitive("foo")));
        simple.setID("foo");
        assertTrue(simple.hasSameID(new JsonPrimitive("foo")));
        assertFalse(simple.hasSameID(new JsonPrimitive("bar")));
        assertTrue(simple.hasSameID(new JsonParser().parse("{\"simple_id\":\"foo\", \"somethingelse\":\"bar\"}")));
        assertFalse(simple.hasSameID(new JsonParser().parse("{\"simple_id\":\"bar\", \"somethingelse\":\"bar\"}")));
        assertFalse(simple.hasSameID(new JsonParser().parse("{\"somethingelse\":\"foo\"}")));
    }
    
    @Test public void testGetExistingModel() {
        JsonElement foo = new JsonPrimitive("foo");
        JsonElement bar = new JsonPrimitive("bar");
        JsonElement fooObj = new JsonParser().parse("{\"simple_id\":\"foo\", \"bar\":\"8\"}");
        Simple[] simples = new Simple[]{ new Simple("blah"), new Simple("foo"), new Simple("baz")};
        assertNull(StackMobModel.getExistingModel(Arrays.asList(new Simple[]{}), foo));
        assertNotNull(StackMobModel.getExistingModel(Arrays.asList(simples), foo));
        assertNotNull(StackMobModel.getExistingModel(Arrays.asList(simples), fooObj));
        assertNull(StackMobModel.getExistingModel(Arrays.asList(simples), bar));
    }

    List<Simple> simples = Arrays.asList(new Simple("blah", "blah", 2), new Simple("foo","foo",3), new Simple("baz","baz",4));
    @Test public void testUpdateModelListFromJson() throws Exception{
        JsonArray trivialUpdate = new JsonParser().parse("[\"blah\", \"foo\", \"baz\"]").getAsJsonArray();
        JsonArray reorderUpdate = new JsonParser().parse("[\"foo\", \"blah\",\"baz\"]").getAsJsonArray();
        JsonArray clearUpdate = new JsonParser().parse("[]").getAsJsonArray();
        JsonArray insertUpdate = new JsonParser().parse("[\"blah\", \"arg\", \"foo\", \"baz\"]").getAsJsonArray();
        JsonArray replaceUpdate = new JsonParser().parse("[{\"simple_id\":\"blah\", \"foo\":\"a\"}, {\"simple_id\":\"foo\", \"foo\":\"b\"}, {\"simple_id\":\"baz\", \"foo\":\"c\"}]").getAsJsonArray();

        List<StackMobModel> trivialUpdated = StackMobModel.updateModelListFromJson(trivialUpdate,simples,Simple.class);
        assertEquals("blah",((Simple)trivialUpdated.get(0)).getID());
        assertEquals("blah",((Simple)trivialUpdated.get(0)).foo);
        assertEquals("foo",((Simple)trivialUpdated.get(1)).getID());
        assertEquals("foo",((Simple)trivialUpdated.get(1)).foo);
        assertEquals("baz",((Simple)trivialUpdated.get(2)).getID());
        assertEquals("baz",((Simple)trivialUpdated.get(2)).foo);

        List<StackMobModel> reorderUpdated = StackMobModel.updateModelListFromJson(reorderUpdate,simples,Simple.class);
        assertEquals("foo",((Simple)reorderUpdated.get(0)).getID());
        assertEquals("foo",((Simple)reorderUpdated.get(0)).foo);
        assertEquals("blah",((Simple)reorderUpdated.get(1)).getID());
        assertEquals("blah",((Simple)reorderUpdated.get(1)).foo);
        assertEquals("baz",((Simple)reorderUpdated.get(2)).getID());
        assertEquals("baz",((Simple)reorderUpdated.get(2)).foo);

        List<StackMobModel> clearUpdated = StackMobModel.updateModelListFromJson(clearUpdate,simples,Simple.class);
        assertTrue(clearUpdated.isEmpty());

        List<StackMobModel> insertUpdated = StackMobModel.updateModelListFromJson(insertUpdate,simples,Simple.class);
        assertEquals("blah",((Simple)insertUpdated.get(0)).getID());
        assertEquals("blah",((Simple)insertUpdated.get(0)).foo);
        assertEquals("arg",((Simple)insertUpdated.get(1)).getID());
        assertEquals("test", ((Simple) insertUpdated.get(1)).foo);
        assertEquals("foo", ((Simple) insertUpdated.get(2)).getID());
        assertEquals("foo",((Simple)insertUpdated.get(2)).foo);
        assertEquals("baz",((Simple)insertUpdated.get(3)).getID());
        assertEquals("baz",((Simple)insertUpdated.get(3)).foo);

        List<StackMobModel> replaceUpdated = StackMobModel.updateModelListFromJson(replaceUpdate,simples,Simple.class);
        assertEquals("blah",((Simple)replaceUpdated.get(0)).getID());
        assertEquals("a",((Simple)replaceUpdated.get(0)).foo);
        assertEquals(2,((Simple)replaceUpdated.get(0)).bar);
        assertEquals("foo",((Simple)replaceUpdated.get(1)).getID());
        assertEquals("b",((Simple)replaceUpdated.get(1)).foo);
        assertEquals(3,((Simple)replaceUpdated.get(1)).bar);
        assertEquals("baz",((Simple)replaceUpdated.get(2)).getID());
        assertEquals("c",((Simple)replaceUpdated.get(2)).foo);
        assertEquals(4,((Simple)replaceUpdated.get(2)).bar);
    }
    
    private static class LotsOfCollections extends StackMobModel {
        public LotsOfCollections(List<Simple> simples) {
            super(LotsOfCollections.class);
            simpleArray = simples.toArray(new Simple[]{});
            simpleList.addAll(simples);
            simpleSet.addAll(simples);
        }

        public LotsOfCollections() {
            super(LotsOfCollections.class);
        }
        
        Simple[] simpleArray = new Simple[3];
        List<Simple> simpleList = new ArrayList<Simple>();
        Set<Simple> simpleSet = new HashSet<Simple>();
    }

    
    @Test public void testGetFieldAsCollection() throws Exception {
        LotsOfCollections loc = new LotsOfCollections(simples);
        Field simpleArray = LotsOfCollections.class.getDeclaredField("simpleArray");
        Field simpleList = LotsOfCollections.class.getDeclaredField("simpleList");
        Field simpleSet = LotsOfCollections.class.getDeclaredField("simpleSet");
        Collection<StackMobModel> result1 = loc.getFieldAsCollection(simpleArray);
        Collection<StackMobModel> result2 = loc.getFieldAsCollection(simpleList);
        Collection<StackMobModel> result3 = loc.getFieldAsCollection(simpleSet);
        assertTrue(result1.contains(simples.get(0)));
        assertTrue(result1.contains(simples.get(1)));
        assertTrue(result1.contains(simples.get(2)));
        assertTrue(result2.contains(simples.get(0)));
        assertTrue(result2.contains(simples.get(1)));
        assertTrue(result2.contains(simples.get(2)));
        assertTrue(result3.contains(simples.get(0)));
        assertTrue(result3.contains(simples.get(1)));
        assertTrue(result3.contains(simples.get(2)));
    }
    
    @Test public void testSetFieldFromList() throws Exception {
        LotsOfCollections loc = new LotsOfCollections();
        loc.simpleList = null;
        Field simpleArray = LotsOfCollections.class.getDeclaredField("simpleArray");
        Field simpleList = LotsOfCollections.class.getDeclaredField("simpleList");
        Field simpleSet = LotsOfCollections.class.getDeclaredField("simpleSet");
        loc.setFieldFromList(simpleArray,simples,Simple.class);
        loc.setFieldFromList(simpleList,simples,Simple.class);
        loc.setFieldFromList(simpleSet,simples,Simple.class);
        assertArrayEquals(simples.toArray(new Simple[]{}),loc.simpleArray);
        assertTrue(loc.simpleList.contains(simples.get(0)));
        assertTrue(loc.simpleList.contains(simples.get(1)));
        assertTrue(loc.simpleList.contains(simples.get(2)));
        assertTrue(loc.simpleSet.contains(simples.get(0)));
        assertTrue(loc.simpleSet.contains(simples.get(1)));
        assertTrue(loc.simpleSet.contains(simples.get(2)));

    }



    /* Online */

    String bookName2 = "yet another book!";
    String bookPublisher2 = "no one";
    final MultiThreadAsserter asserter = new MultiThreadAsserter();
    final CountDownLatch latch = latchOne();

    private abstract class AssertErrorCallback extends StackMobCallback {

        @Override
        public void failure(StackMobException e) {
            asserter.markException(e);
        }
    }
    
    @Test public void testSaveToServer()  throws Exception {
        final Book book = new Book();
        book.setTitle(bookName2);
        book.setPublisher(bookPublisher2);
        
        assertNull(book.getID());

        book.save(new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotNull(book.getID());
                latch.countDown();
            }
        });

        asserter.assertLatchFinished(latch);
    }

    @Test public void saveComplicatedTypesToServer()  throws Exception {
        final Complicated ls = new Complicated();

        ls.save(new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotNull(ls.getID());
                latch.countDown();
            }
        });

        asserter.assertLatchFinished(latch);
    }



    @Test public void testUpdateFromServer() throws Exception {
        final Book book = new Book();
        book.setID("4f6a1660625498bf90571c36");
        book.fetch(new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                System.out.println("success");
                asserter.markEquals(book.getTitle(), bookName2);
                asserter.markEquals(book.getPublisher(), bookPublisher2);
                latch.countDown();
            }
        });

        asserter.assertLatchFinished(latch);
    }

    @Test public void testDeepSave() throws Exception {
        Library lib = new Library();
        lib.name = "SF Public Library";
        Author a = new Author("Tolstoy");
        Book b1 = new Book("War and Peace","foo", a);
        Book b2 = new Book("Anna Karenina", "bar", a);
        lib.books = new Book[] {b1, b2};
        lib.saveWithDepth(2, new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                
                latch.countDown();
            }
        });
        asserter.assertLatchFinished(latch);
    }


    @Test public void testFullSequence() throws Exception {
        final Author author = new Author();
        author.setName("Larry Wall");
        author.save(new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                createBook(author);
            }
        });
        
        asserter.assertLatchFinished(latch);
    }
    
    
    public void createBook(Author author) {
        final Book book = new Book();
        book.setID("camelbook");
        book.setTitle("Programming Perl");
        book.setPublisher("O'Reilly");
        book.setAuthor(author);
        book.save(new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                fetchBook();
            }
        });
    }
    
    public void fetchBook() {
        final Book book = new Book();
        book.setID("camelbook");
        book.fetch(new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                fetchBookWithExpand();
            }
        });
    }
    
    public void fetchBookWithExpand() {
        final Book book = new Book();
        book.setID("camelbook");
        book.fetchWithDepth(2, new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                updateBook(book);
            }
        });
    }
    
    public void updateBook(Book book) {
        final Book theBook = book;
        book.setTitle("Programming Perl 2: Perl Harder");
        book.save(new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                deleteBook(theBook);
            }
        });
    }

    public void deleteBook(Book book) {
        book.destroy(new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                latch.countDown();
            }
        });
    }

    public static class Task extends StackMobModel {

        private String name;
        private Date dueDate;
        private int priority = 0;
        private boolean done = false;

        public Task() {
            super(Task.class);
        }

        public Task(String name) {
            this();
            this.name = name;
        }
    }

    public static class TaskList extends StackMobModel {

        private String name;
        private List<Task> tasks = new ArrayList<Task>();

        public TaskList() {
            super(TaskList.class);
        }

        public TaskList(String name) {
            this();
            this.name = name;
        }
    }

    @Test public void testMultiSave() throws Exception {
        final TaskList tl = new TaskList("TODO");
        final Task t = new Task("fix this api");
        final Task t2 = new Task("make it work");
        final List<Task> oldTaskList = tl.tasks;
        tl.tasks.add(t);
        tl.tasks.add(t2);
        tl.saveWithDepth(1,new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotNull(tl.getID());
                asserter.markNotNull(tl.tasks.get(0).getID());
                asserter.markNotNull(tl.tasks.get(1).getID());
                asserter.markTrue(tl.tasks.get(0) == t);
                asserter.markTrue(tl.tasks.get(1) == t2);
                asserter.markTrue(tl.tasks == oldTaskList);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    public static class CounterTest extends StackMobModel {
        public CounterTest(){
            super(CounterTest.class);
        }
        StackMobCounter counter = new StackMobCounter();
    }

    @Test public void testCounter() throws Exception {
        final CounterTest counter = new CounterTest();
        counter.save(new AssertErrorCallback() {
            @Override
            public void success(String responseBody) {
                JsonObject result = new JsonParser().parse(responseBody).getAsJsonObject();
                asserter.markEquals(0, result.get("counter").getAsInt());
                counter.counter.updateAtomicallyBy(5);
                counter.save(new AssertErrorCallback() {
                    @Override
                    public void success(String responseBody) {
                        JsonObject result = new JsonParser().parse(responseBody).getAsJsonObject();
                        asserter.markEquals(5, result.get("counter").getAsInt());
                        counter.counter.forceTo(-1);
                        counter.save(new AssertErrorCallback() {
                            @Override
                            public void success(String responseBody) {
                                JsonObject result = new JsonParser().parse(responseBody).getAsJsonObject();
                                asserter.markEquals(-1, result.get("counter").getAsInt());
                                counter.counter.updateAtomicallyBy(4);
                                counter.save(new AssertErrorCallback() {
                                    @Override
                                    public void success(String responseBody) {
                                        JsonObject result = new JsonParser().parse(responseBody).getAsJsonObject();
                                        asserter.markEquals(3, result.get("counter").getAsInt());
                                        counter.counter = new StackMobCounter();
                                        counter.counter.forceTo(5);
                                        counter.counter.updateAtomicallyBy(1);
                                        counter.fetch(new AssertErrorCallback() {
                                            @Override
                                            public void success(String responseBody) {
                                                JsonObject result = new JsonParser().parse(responseBody).getAsJsonObject();
                                                asserter.markEquals(3, result.get("counter").getAsInt());
                                                counter.counter.forceTo(5);
                                                counter.counter.updateAtomicallyBy(1);
                                                counter.save(new AssertErrorCallback() {
                                                    @Override
                                                    public void success(String responseBody) {
                                                        JsonObject result = new JsonParser().parse(responseBody).getAsJsonObject();
                                                        asserter.markEquals(6, result.get("counter").getAsInt());

                                                        latch.countDown();
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }

        });
        asserter.assertLatchFinished(latch);
    }
}
