dependencies {
    compile(project(":lib"))
    compile(project(":annotations"))

    implementation( "net.jcip", "jcip-annotations", "1.0")
    implementation( "com.google.code.findbugs", "jsr305", "3.0.2")

    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_7
}