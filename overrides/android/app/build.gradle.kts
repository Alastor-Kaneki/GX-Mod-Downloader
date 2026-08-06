import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val iconArchive = rootProject.file("../assets/gx-icon-pack.zip")
val generatedIconRes = layout.buildDirectory.dir("generated/icon-pack/res")
val generatedIconAssets = layout.buildDirectory.dir("generated/icon-pack/assets")

fun resourceId(path: String): String {
    val parts = path.split('/')
    val family = parts[parts.size - 2]
    val stem = parts.last().substringBeforeLast('.').replace('-', '_')
    return (family + "_" + stem).lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
}

val generateIconPack by tasks.registering {
    inputs.file(iconArchive)
    outputs.dir(generatedIconRes)
    outputs.dir(generatedIconAssets)
    doLast {
        val resDir = generatedIconRes.get().asFile.resolve("mipmap-xxxhdpi")
        val assetsDir = generatedIconAssets.get().asFile.resolve("icons")
        resDir.deleteRecursively()
        assetsDir.deleteRecursively()
        resDir.mkdirs()
        assetsDir.mkdirs()
        ZipFile(iconArchive).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("app_icon/") && it.name.endsWith(".png") }
                .forEach { entry ->
                    val id = resourceId(entry.name)
                    val bytes = zip.getInputStream(entry).readBytes()
                    resDir.resolve("ic_theme_${id}.png").writeBytes(bytes)
                    assetsDir.resolve("${id}.png").writeBytes(bytes)
                }
        }
    }
}

android {
    namespace = "dev.alastorkaneki.gxmods"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.alastorkaneki.gxmods"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.1-alpha"
    }

    sourceSets.getByName("main") {
        res.srcDir(generatedIconRes)
        assets.srcDirs(rootProject.file("../shared"), generatedIconAssets)
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }
}

tasks.named("preBuild").configure { dependsOn(generateIconPack) }
