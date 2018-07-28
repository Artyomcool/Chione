apply<GroovyPlugin>()

dependencies {
    compile(project(":apt-lib"))

    testCompile("junit", "junit", "4.12")
    testCompile("pl.pragmatists", "JUnitParams", "1.1.1")
    testCompile("com.google.testing.compile", "compile-testing", "0.8")
    testCompile("com.google.guava", "guava", "19.0")
    testCompile("org.codehaus.groovy", "groovy-all", "2.4.6")
    testCompile("com.github.javaparser", "javaparser-core", "3.2.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}