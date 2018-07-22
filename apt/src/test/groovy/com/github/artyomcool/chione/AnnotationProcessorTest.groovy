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
        return generate(sources).loadClass(name).newInstance(new InMemoryDataFile()) as ChioneModule<?, ?>
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

}
