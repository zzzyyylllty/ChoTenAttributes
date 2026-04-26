# attribute

超天属性管理系统服务 (Liminal Skyline v4.0 服务)

ChoTen Attribute management system service (Liminal Skyline v4.0 Service)

不是很好用的属性库，因开服迫切使用了部分Claude Code，暂无时间优化。

## As dependency

```Gradle kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.zzzyyylllty:attribute-Hydrochloride:VERSION")
}
```

## Build Runtime Version

Required Java 21.

Runtime version for normal use.

Build artifact is in `plugin/build/libs` folder.

```
./gradlew clean build
```

## Build Api Version

The api version includes the TabooLib core, intended for developers' use but not runnable.

```
./gradlew clean taboolibBuildApi -PDeleteCode
```

> The parameter `-PDeleteCode` indicates the removal of all logic code to reduce size.
