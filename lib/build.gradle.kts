
dependencies {
    implementation( "net.jcip", "jcip-annotations", "1.0")
    implementation( "com.google.code.findbugs", "jsr305", "3.0.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_7
}