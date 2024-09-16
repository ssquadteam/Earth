allprojects {
    group = "com.github.ssquadteam.earth"
    version = "0.0.1"
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://oss.sonatype.org/content/repositories/central")
        flatDir {
            dirs("$rootDir/lib")
        }
    }
}
