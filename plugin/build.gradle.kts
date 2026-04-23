taboolib {
    description {
        name("ChoTenAttributes")
        desc("ChoTen Attribute System.")
        contributors {
            name("AkaCandyKAngel")
        }
        dependencies {
            name("DylsemHokma").optional(true)
            name("packetevents").optional(true)
            name("PlaceholderAPI").optional(true)
        }
    }

    relocate("top.maplex.arim", "io.github.zzzyyylllty.attribute.dep.arim")
    relocate("ink.ptms.um", "io.github.zzzyyylllty.attribute.dep.um")
    relocate("com.alibaba", "io.github.zzzyyylllty.attribute.dep.alibaba")
    relocate("kotlinx.serialization", "kotlinx.serialization181")
    relocate("io.github.projectunified.uniitem", "io.github.zzzyyylllty.attribute.dep.uniitem")
    relocate("com.fasterxml.jackson", "io.github.zzzyyylllty.attribute.dep.jackson")
    relocate("com.mojang.datafixers", "io.github.zzzyyylllty.attribute.dep.datafixers")
    relocate("io.netty.handler.codec.http", "io.github.zzzyyylllty.attribute.dep.http")
    relocate("io.netty.handler.codec.rtsp", "io.github.zzzyyylllty.attribute.dep.rtsp")
    relocate("io.netty.handler.codec.spdy", "io.github.zzzyyylllty.attribute.dep.spdy")
    relocate("io.netty.handler.codec.http2", "io.github.zzzyyylllty.attribute.dep.http2")
    relocate("org.tabooproject.fluxon", "io.github.zzzyyylllty.attribute.dep.fluxon")
    relocate("com.github.benmanes.caffeine", "io.github.zzzyyylllty.attribute.dep.caffeine")
    relocate("org.kotlincrypto", "io.github.zzzyyylllty.attribute.dep.kotlincrypto")
//    relocate("org.objectweb.asm", "io.github.zzzyyylllty.attribute.dep.asm")
}


tasks {
    val taboolibMainTask = named("taboolibMainTask")

    jar {
        archiveFileName.set("${rootProject.name}-${rootProject.version}-Premium.jar")
        rootProject.subprojects.forEach { from(it.sourceSets["main"].output) }
    }

    val freeJar by registering(Jar::class) {
        group = "build"
        description = "Generate FREE version jar by filtering premium classes"

        dependsOn(taboolibMainTask)
        dependsOn(jar)

        archiveFileName.set("${rootProject.name}-${version}-Free.jar")

        // 修复：使用 archiveFile 替代 archivePath
        from(zipTree(jar.get().archiveFile)) {
            exclude("io/github/zzzyyylllty/attribute/premium/**")
        }
    }

    named("build") {
        // paperweight dependsOn(reobfJar)
        dependsOn(freeJar)
    }
}