allprojects {
    group = "com.github.artyomcool"
    version = "1.0-SNAPSHOT"

    apply {
        plugin(JavaPlugin::class.java)
    }

    repositories {
        mavenCentral()
    }
}
