package com.github.artyomcool.chione

import com.github.javaparser.JavaParser
import com.google.testing.compile.JavaFileObjects
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith

import javax.tools.*
import java.nio.charset.Charset

@RunWith(JUnitParamsRunner)
class AnnotationProcessorTest {

    private ClassLoader generate(String... sources) {

        def objects = sources.collect { String code ->
            def unit = JavaParser.parse(code)
            def packageName = unit.packageDeclaration.get()?.name?.toString()
            def className = unit.getType(0).name.toString()
            def fullName = packageName == null ? className : "$packageName.$className"

            JavaFileObjects.forSourceLines(fullName, code)
        }
        def compiler = ToolProvider.getSystemJavaCompiler();

        def generatedSources = []
        def generatedClasses = []

        def stdFileManager = compiler.getStandardFileManager({}, Locale.ENGLISH, Charset.forName("UTF-8"))
        JavaFileManager inMemoryFileManager = Class.forName("com.google.testing.compile.InMemoryJavaFileManager").newInstance(stdFileManager) as JavaFileManager

        JavaFileManager fileManager = new ForwardingJavaFileManager(inMemoryFileManager) {
            JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
                switch (location.name) {
                    case StandardLocation.SOURCE_OUTPUT.name:
                        generatedSources << className
                        break
                    case StandardLocation.CLASS_OUTPUT.name:
                        generatedClasses << className
                        break
                }
                return super.getJavaFileForOutput(location, className, kind, sibling)
            }
        }

        def diagnostics = []

        def task = compiler.getTask(null, fileManager, { diagnostics << it }, [], [], objects);

        task.setProcessors([new AnnotationProcessor()])

        try {
            if (!task.call()) {
                throw new IllegalStateException("Compilation error");
            }

            ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
                @Override
                Class<?> findClass(String name) throws ClassNotFoundException {
                    if (name in generatedClasses) {
                        def bytes = fileManager.getJavaFileForOutput(
                                [getName: { 'CLASS_OUTPUT' }] as JavaFileManager.Location,
                                name,
                                JavaFileObject.Kind.CLASS,
                                null
                        ).openInputStream().bytes
                        return defineClass(name, bytes, 0, bytes.length)
                    }
                    throw new ClassNotFoundException("Class not found: " + name)
                }
            }
            def sources1 = getSources(inMemoryFileManager, generatedSources)
            println sources1
            println()
            println diagnostics.join("\n")
            println()

            return loader
        } catch (Exception e) {
            def sources1 = getSources(inMemoryFileManager, generatedSources)
            println sources1
            println()
            println diagnostics.join("\n")
            println()
            throw new IllegalStateException(e);
        }

    }

    static getSources(JavaFileManager fileManager, List<String> generatedSources) {
        generatedSources.collect {
            "====== File $it ======\n" + fileManager.getJavaFileForOutput(
                    [getName: { 'SOURCE_OUTPUT' }] as JavaFileManager.Location,
                    it,
                    JavaFileObject.Kind.SOURCE,
                    null
            ).getCharContent(true)
        }.join('\n\n')
    }

    ChioneModule<?, ?> generateModule(String name, String... sources) {
        return generateModule(new InMemoryDataFile(), name, sources)
    }

    ChioneModule<?, ?> generateModule(DataFile dataFile, String name, String... sources) {
        return generate(sources).loadClass(name).newInstance(dataFile) as ChioneModule<?, ?>
    }

    ChioneModule<?, ?> oneFieldModule(String type) {
        def someEntryClass =
                """
                    package test;

                    import com.github.artyomcool.chione.Ice;
                    
                    @Ice
                    public interface SomeEntry {
                        
                        $type data();
                        
                        void data($type t);
                        
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

        return generateModule("test.SomeFactoryModule", someEntryClass, factoryClass)
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

    @Test
    @Parameters(["String", "Object", "Integer", "Double"])
    void arrayList(String type) {
        def module = oneFieldModule("java.util.ArrayList<$type>")
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()

        def name = type.contains("<") ? type.substring(0, type.indexOf("<")) : type;
        def clazz = Class.forName(name.contains(".") ? name : "java.lang.$name")

        def original = [-100, -1, 0, 1, 2, 3, 4, 5].collect { it.asType(clazz) }
        entry.data(original)
        chione.save(entry)

        def nextEntry = chione.load()

        assert entry != nextEntry
        assert !(entry.data().is(nextEntry.data()))
        assert entry.data() == nextEntry.data()
        assert nextEntry.data() == original
    }

    @Test
    void arrayListOfArrayList() {
        def module = oneFieldModule("java.util.ArrayList<java.util.ArrayList<String>>")
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()

        def original = [["one", "two"], ["three"]]
        entry.data(original)
        chione.save(entry)

        def nextEntry = chione.load()

        assert entry != nextEntry
        assert !(entry.data().is(nextEntry.data()))
        assert entry.data() == nextEntry.data()
        assert nextEntry.data() == original
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
    void lazy() {
        def module = oneFieldModule("com.github.artyomcool.chione.Lazy<String>")
        def factory = module.factory()
        def chione = module.chione()

        def entry = factory.createEntry()
        entry.data(new Lazy<>("Hey!"))

        chione.save(entry)

        def nextEntry = chione.load()

        assert !nextEntry.data().isLoaded()
        assert nextEntry.data().get() == "Hey!"
        assert nextEntry.data().isLoaded()
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

}
