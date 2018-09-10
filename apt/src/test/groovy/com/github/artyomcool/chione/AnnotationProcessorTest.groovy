/*
 * The MIT License
 *
 * Copyright (c) 2018 Artyom Drozdov (https://github.com/artyomcool)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.artyomcool.chione

import com.github.javaparser.JavaParser
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith

import javax.tools.StandardLocation

@RunWith(JUnitParamsRunner)
class AnnotationProcessorTest {

    private static String unindent(String code) {
        int indent = -1
        StringBuilder builder = new StringBuilder();
        code.eachLine {
            if (indent == -1) {
                if (!it.isAllWhitespace()) {
                    indent = it.length() - it.trim().length()
                    builder.append(it.substring(indent)).append("\n");
                }
            } else {
                if (it.isAllWhitespace()) {
                    builder.append("\n")
                } else {
                    builder.append(it.substring(indent)).append("\n");
                }
            }
        }

        return builder.toString()
    }

    private ClassLoader generate(String... sources) {

        def objects = sources.collect { String code ->
            code = unindent(code)
            def unit = JavaParser.parse(code)
            def packageName = unit.packageDeclaration.get()?.name?.toString()
            def className = unit.getType(0).name.toString()
            def fullName = packageName == null ? className : "$packageName.$className"

            JavaFileObjects.forSourceLines(fullName, code)
        }

        def compilation = Compiler.javac()
                .withProcessors(new AnnotationProcessor())
                .compile(objects)

        CompilationSubject.assertThat(compilation).succeeded()

        return new ClassLoader(getClass().getClassLoader()) {
            @Override
            Class<?> findClass(String name) throws ClassNotFoundException {
                def className = name.replace(".", "/") + ".class"
                def file = compilation.generatedFile(StandardLocation.CLASS_OUTPUT, className)
                        .orElseThrow({ new ClassNotFoundException("Class not found: " + name) })
                def bytes = file.openInputStream().bytes
                return defineClass(name, bytes, 0, bytes.length)
            }
        }
    }

    ChioneModule<?, ?> generateModule(String name, String... sources) {
        return generateModule(new InMemoryDataFile(), name, sources)
    }

    ChioneModule<?, ?> generateModule(DataFile dataFile, String name, String... sources) {
        return generate(sources).loadClass(name).newInstance(dataFile) as ChioneModule<?, ?>
    }

    ChioneModule<?, ?> oneEntryModule(String entry) {
        def factoryClass =
                """
                    package test;
                    
                    import com.github.artyomcool.chione.Factory;
                    
                    @Factory(root = SomeEntry.class)
                    public interface SomeFactory {
                        
                        SomeEntry createEntry();
                        
                    }
                """

        return generateModule("test.SomeFactoryModule", entry, factoryClass)
    }

    ChioneModule<?, ?> oneFieldModule(String type) {
        oneEntryModule """
                    package test;

                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeEntry {
                        
                        $type data();
                        
                        void data($type t);
                        
                    }

                """
    }

    @Test
    @Parameters(["String"])
    void object(String type) {
        def module = oneFieldModule(type)
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()

        def clazz = entry.getClass().getMethod("data").returnType;
        def original = 90.asType(clazz)

        entry.data(original)
        chione.save(entry)

        def nextEntry = chione.load()

        assert entry != nextEntry
        assert !(entry.data().is(nextEntry.data()))
        assert entry.data() == nextEntry.data()
        assert nextEntry.data() == original
    }

    @Test
    @Parameters(["boolean", "byte", "short", "char", "int", "float", "long", "double"])
    void primitive(String type) {
        def module = oneFieldModule(type)
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()

        def clazz = entry.getClass().getMethod("data").returnType;
        def original = 90.asType(clazz)

        entry.data(original)
        chione.save(entry)

        def nextEntry = chione.load()

        assert entry != nextEntry
        assert entry.data() == nextEntry.data()
        assert nextEntry.data() == original
    }

    @Test
    @Parameters(["Boolean", "Byte", "Short", "Character", "Integer", "Float", "Long", "Double"])
    void wrapper(String type) {
        def module = oneFieldModule("java.lang.$type")
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()

        def clazz = entry.getClass().getMethod("data").returnType;
        def original = 90.asType(clazz)

        entry.data(original)
        chione.save(entry)

        def nextEntry = chione.load()

        assert entry != nextEntry
        assert entry.data() == nextEntry.data()
        assert nextEntry.data() == original
    }

    @Test
    @Parameters(["boolean", "byte", "short", "char", "int", "float", "long", "double", "String", "String[]"])
    void array(String type) {
        def module = oneFieldModule("$type[]")
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()

        def clazz = entry.getClass().getMethod("data").returnType;

        def original = [-100, -1, 0, 1, 2, 3, 4, 5].asType(clazz)
        entry.data(original)
        chione.save(entry)

        def nextEntry = chione.load()

        assert entry != nextEntry
        assert !(entry.data().is(nextEntry.data()))
        assert entry.data() == nextEntry.data()
        assert nextEntry.data() == original
    }

    @Test
    void selfReferencing() {
        def module = oneFieldModule("SomeEntry")
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()
        entry.data(entry)
        chione.save(entry)

        def nextEntry = chione.load()

        assert !entry.is(nextEntry)
        assert entry.data().is(entry)
        assert nextEntry.data().is(nextEntry)
    }

    @Test
    void recursiveReferencing() {
        def someEntryClass =
                """
                    package test;

                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeEntry {
                        
                        SomeAnotherEntry anotherEntry();
                        
                        void anotherEntry(SomeAnotherEntry entry);
                        
                        String tagToVerify();
                        
                        void tagToVerify(String tag);
                        
                    }

                """

        def someAnotherEntryClass =
                """
                    package test;

                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeAnotherEntry {
                        
                        SomeEntry entry();
                        
                        void entry(SomeEntry entry);
                        
                        String tagToVerify();
                        
                        void tagToVerify(String tag);
                        
                    }

                """

        def factoryClass =
                """
                    package test;
                    
                    import com.github.artyomcool.chione.Factory;
                    
                    @Factory(root = SomeEntry.class)
                    public interface SomeFactory {
                        
                        SomeEntry createEntry();
                        
                        SomeAnotherEntry createAnotherEntry();
                        
                    }
                """

        def module = generateModule("test.SomeFactoryModule", someEntryClass, someAnotherEntryClass, factoryClass)
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()
        def anotherEntry = factory.createAnotherEntry()

        entry.anotherEntry(anotherEntry)
        anotherEntry.entry(entry)

        entry.tagToVerify("Entry tag")
        anotherEntry.tagToVerify("Another entry tag")

        chione.save(entry)

        def nextEntry = chione.load()
        def nextAnotherEntry = nextEntry.anotherEntry()

        assert !entry.is(nextEntry)
        assert !anotherEntry.is(nextAnotherEntry)

        assert nextEntry.is(nextAnotherEntry.entry())

        assert nextEntry.tagToVerify() == "Entry tag"
        assert anotherEntry.tagToVerify() == "Another entry tag"
    }

    private void testCollection(String type, def originalData) {
        def module = oneFieldModule(type)
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()

        entry.data(originalData)
        chione.save(entry)

        def nextEntry = chione.load()

        assert entry != nextEntry
        assert !(entry.data().is(nextEntry.data()))
        assert entry.data() == nextEntry.data()
        assert nextEntry.data() == originalData
    }

    @Test
    @Parameters(["String", "Object", "Integer", "Double"])
    void arrayList(String type) {
        def clazz = Class.forName("java.lang.$type")

        def original = [-100, -1, 0, 1, 2, 3, 4, 5].collect { it.asType(clazz) } as ArrayList

        testCollection("java.util.ArrayList<$type>", original)
    }

    @Test
    void arrayListOfArrayList() {
        def original = [["one", "two"], ["three"]] as ArrayList
        testCollection("java.util.ArrayList<java.util.ArrayList<String>>", original)
    }

    @Test
    void recursiveArrayList() {
        def module = oneFieldModule("java.util.ArrayList<Object>")
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()

        def original = ["First tag"] as ArrayList<Object>
        original.add(original)
        original.add("Last tag")

        entry.data(original)
        chione.save(entry)

        def nextEntry = chione.load()
        List<?> nextData = nextEntry.data()

        assert nextData.size() == 3
        assert nextData[0] == "First tag"
        assert nextData[1] == nextData
        assert nextData[2] == "Last tag"
    }

    @Test
    @Parameters(["String", "Object", "Integer", "Double"])
    void linkedList(String type) {
        def clazz = Class.forName("java.lang.$type")

        def original = [-100, -1, 0, 1, 2, 3, 4, 5].collect { it.asType(clazz) } as LinkedList

        testCollection("java.util.LinkedList<$type>", original)
    }

    @Test
    @Parameters(["String", "Object", "Integer", "Double"])
    void hashSet(String type) {
        def clazz = Class.forName("java.lang.$type")

        def original = [-100, -1, 0, 1, 2, 3, 4, 5].collect { it.asType(clazz) } as HashSet

        testCollection("java.util.HashSet<$type>", original)
    }

    @Test
    @Parameters(["String", "Object", "Integer", "Double"])
    void linkedHashSet(String type) {
        def clazz = Class.forName("java.lang.$type")

        def original = [-100, -1, 0, 1, 2, 3, 4, 5].collect { it.asType(clazz) } as LinkedHashSet

        testCollection("java.util.LinkedHashSet<$type>", original)
    }

    @Test
    @Parameters(["String", "Object", "Integer", "Double"])
    void hashMapKey(String type) {
        def clazz = Class.forName("java.lang.$type")

        def original = new HashMap([-100, -1, 0, 1, 2, 3, 4, 5].collectEntries { [(it.asType(clazz)) : it] })

        testCollection("java.util.HashMap<$type, Integer>", original)
    }

    @Test
    @Parameters(["String", "Object", "Integer", "Double"])
    void hashMapValue(String type) {
        def clazz = Class.forName("java.lang.$type")

        def original = new HashMap([-100, -1, 0, 1, 2, 3, 4, 5].collectEntries { [(it) : it.asType(clazz)] })

        testCollection("java.util.HashMap<Integer, $type>", original)
    }

    @Test
    @Parameters(["String", "Object", "Integer", "Double"])
    void linkedHashMapKey(String type) {
        def clazz = Class.forName("java.lang.$type")

        def original = new LinkedHashMap([-100, -1, 0, 1, 2, 3, 4, 5].collectEntries { [(it.asType(clazz)) : it] })

        testCollection("java.util.LinkedHashMap<$type, Integer>", original)
    }

    @Test
    @Parameters(["String", "Object", "Integer", "Double"])
    void linkedHashMapValue(String type) {
        def clazz = Class.forName("java.lang.$type")

        def original = new LinkedHashMap([-100, -1, 0, 1, 2, 3, 4, 5].collectEntries { [(it) : it.asType(clazz)] })

        testCollection("java.util.LinkedHashMap<Integer, $type>", original)
    }

    @Test
    void listHierarchy() {
        def original = new LinkedList([-100, -1, 0, 1, 2, 3, 4, 5]) {
            @Override
            String toString() {
                return "just for fun"
            }
        }

        testCollection("java.util.List<Integer>", original)
    }

    @Test
    void setHierarchy() {
        def original = new TreeSet([-100, -1, 0, 1, 2, 3, 4, 5]) {
            @Override
            String toString() {
                return "just for fun"
            }
        }

        testCollection("java.util.Set<Integer>", original)
    }

    @Test
    void mapHierarchy() {
        def original = new HashMap([-100, -1, 0, 1, 2, 3, 4, 5].collectEntries { [(it) : it.toString()] }) {
            @Override
            String toString() {
                return "just for fun"
            }
        }

        testCollection("java.util.Map<Integer, String>", original)
    }

    @Test
    void fieldRemoval() {
        def fullData =
                """
                    package test;

                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeEntry {
                        
                        String field1();
                        
                        void field1(String data);
                        
                        String field2();
                        
                        void field2(String data);
                      
                        String field3();
                        
                        void field3(String data);
                        
                    }

                """

        def noField2Data =
                """
                    package test;

                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeEntry {
                        
                        String field1();
                        
                        void field1(String data);
                        
                        String field3();
                        
                        void field3(String data);
                        
                    }

                """

        def factoryClass =
                """
                    package test;
                    
                    import com.github.artyomcool.chione.Factory;
                    
                    @Factory(root = SomeEntry.class)
                    public interface SomeFactory {
                        
                        SomeEntry createEntry();
                        
                    }
                """

        def dataFile = new InMemoryDataFile()
        def fullModule = generateModule(dataFile,"test.SomeFactoryModule", fullData, factoryClass)
        def noField2Module = generateModule(dataFile,"test.SomeFactoryModule", noField2Data, factoryClass)

        def entry = fullModule.factory().createEntry()
        entry.field1("f1")
        entry.field2("f2")
        entry.field3("f3")

        fullModule.chione().save(entry)

        def nextEntry = noField2Module.chione().load()

        assert nextEntry.field1() == "f1"
        assert nextEntry.field3() == "f3"

        try {
            nextEntry.field2()

            throw new IllegalStateException("Method field2 still generated")
        } catch (MissingMethodException ignored) {
        }
    }

    @Test
    void fieldAddition() {
        def fullData =
                """
                    package test;

                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeEntry {
                        
                        String field1();
                        
                        void field1(String data);
                        
                        String field2();
                        
                        void field2(String data);
                      
                        String field3();
                        
                        void field3(String data);
                        
                    }

                """

        def noField2Data =
                """
                    package test;

                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeEntry {
                        
                        String field1();
                        
                        void field1(String data);
                        
                        String field3();
                        
                        void field3(String data);
                        
                    }

                """

        def factoryClass =
                """
                    package test;
                    
                    import com.github.artyomcool.chione.Factory;
                    
                    @Factory(root = SomeEntry.class)
                    public interface SomeFactory {
                        
                        SomeEntry createEntry();
                        
                    }
                """

        def dataFile = new InMemoryDataFile()
        def fullModule = generateModule(dataFile,"test.SomeFactoryModule", fullData, factoryClass)
        def noField2Module = generateModule(dataFile,"test.SomeFactoryModule", noField2Data, factoryClass)

        def entry = noField2Module.factory().createEntry()
        entry.field1("f1")
        entry.field3("f3")

        noField2Module.chione().save(entry)

        def nextEntry = fullModule.chione().load()

        assert nextEntry.field1() == "f1"
        assert nextEntry.field2() == null
        assert nextEntry.field3() == "f3"
    }

    @Test
    void lazy() {
        def module = oneFieldModule("com.github.artyomcool.chione.Lazy<String>")
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()
        entry.data(new Lazy<>("Hey!"))

        chione.save(entry)

        def nextEntry = chione.load()

        assert !nextEntry.data().isLoaded()
        assert nextEntry.data().lazy != "Hey!"
        assert nextEntry.data().get() == "Hey!"
        assert nextEntry.data().lazy == "Hey!"
        assert nextEntry.data().isLoaded()
    }

    @Test
    @Parameters([
            "boolean",
            "byte",
            "short",
            "char",
            "int",
            "float",
            "long",
            "double",
            "String",
            "String[]",
            "Boolean",
            "Integer"
    ])
    void simpleLazyAnnotatedField(def type) {
        def module = oneEntryModule """
                    package test;

                    import com.github.artyomcool.chione.Fetch;
                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeEntry {
                        
                        @Fetch(Fetch.Type.LAZY) 
                        $type data();
                        
                        void data($type t);
                        
                    }

                """

        testLazy(module)
    }

    @Test
    @Parameters([
            "String",
            "String[]",
            "Boolean",
            "Integer"
    ])
    void lazyAnnotatedClass(def type) {
        def module = oneEntryModule """
                    package test;

                    import com.github.artyomcool.chione.Fetch;
                    import com.github.artyomcool.chione.Ice;
                    
                    @Fetch(Fetch.Type.LAZY) 
                    @Ice
                    public interface SomeEntry {
                       
                        $type data();
                        
                        void data($type t);
                        
                    }

                """
        testLazy(module)
    }

    @Test
    @Parameters([
            "boolean",
            "byte",
            "short",
            "char",
            "int",
            "float",
            "long",
            "double"
    ])
    void lazyAnnotatedClassWithPrimitives(def type) {
        def module = oneEntryModule """
                    package test;

                    import com.github.artyomcool.chione.Fetch;
                    import com.github.artyomcool.chione.Ice;
                    
                    @Fetch(Fetch.Type.LAZY) 
                    @Ice
                    public interface SomeEntry {
                       
                        $type data();
                        
                        void data($type t);
                        
                    }

                """

        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()
        def clazz = entry.getClass().getMethod("data").returnType;
        def fieldType = entry.getClass().getDeclaredField("data").type

        assert fieldType == clazz

        def original = 90.asType(clazz)

        entry.data(original)

        chione.save(entry)

        def nextEntry = chione.load()

        assert nextEntry.data == original
    }

    private static void testLazy(ChioneModule<?, ?> module) {

        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()
        def clazz = entry.getClass().getMethod("data").returnType;
        def original = 90.asType(clazz)

        entry.data(original)

        chione.save(entry)

        def nextEntry = chione.load()

        assert nextEntry.data instanceof Lazy
        assert nextEntry.data.lazy != original
        assert !nextEntry.data.isLoaded()
        assert nextEntry.data() == original
        assert nextEntry.data.lazy == original
        assert nextEntry.data.isLoaded()
    }


    @Test
    @Parameters([
            "boolean",
            "byte",
            "short",
            "char",
            "int",
            "float",
            "long",
            "double",
            "String",
            "String[]",
            "Boolean",
            "Integer"
    ])
    void builder(def type) {
        def factoryClass =
                """
                    package test;
                    
                    import com.github.artyomcool.chione.Factory;
                    
                    @Factory(root = SomeEntry.class)
                    public interface SomeFactory {
                        
                        SomeEntry.Builder entryBuilder();
                        
                    }
                """

        def moduleClass =  """
                    package test;

                    import com.github.artyomcool.chione.Fetch;
                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeEntry {
                       
                        $type data();
                        
                        @Ice.Builder
                        public interface Builder {
                            Builder data($type data);
                            SomeEntry build();
                        }
                        
                    }

                """

        def module = generateModule("test.SomeFactoryModule", moduleClass, factoryClass)

        def factory = module.factory()
        def chione = module.chione()

        def builder = factory.entryBuilder()
        def clazz = builder.getClass().getDeclaredField("data").type;
        def original = 90.asType(clazz)

        def entry = builder.data(original).build()

        chione.save(entry)

        def nextEntry = chione.load()

        assert nextEntry.data() == original
    }


}
