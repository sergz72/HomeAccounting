plugins {
    kotlin("jvm") version "2.1.20"
}

group = "com.sz.home_accounting.core"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    api("org.apache.commons:commons-compress:1.27.1")
    implementation(files("../../smart_home/smart_home_common/build/libs/smart_home_common-0.1.jar"))
    implementation(files("../../file_server/file_server_lib/build/libs/file_server_lib-0.1.jar"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(sourceSets.main.get().output)
}
