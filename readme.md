# sbt-matrix-sources

Source layouts for `projectMatrix` in sbt 2.

The plugin supports only one layout today, the one that is compatible with what
[`sbt-crossproject`](https://github.com/portable-scala/sbt-crossproject) defines.
If you move a build from `crossProject` to `projectMatrix`,
this plugin will allow every source file to remain where it is.

The plugin only adds directories (as described below), it doesn't remove any that are
defined by `projectMatrix` or the user.

Every row of a matrix gets `<platform_part>/src/main/<language_part>` (with the same under
`src/test`), where:

- `<platform_part>` is one of:
  - `shared`: covers all platforms
  - each configured platform axis, such as `jvm`, `js` or `native`
  - each sorted partial sub-group of platforms, such as `js-jvm`
- `<language_part>` is one of:
  - `scala`
  - `scala-<version_prefix>`, one for each prefix of the row's Scala version
    - e.g., for `scala-2.13.18`, includes the version plus `scala-2.13` and `scala-2`
  - `java` (only under the row's own platform)
  - `resources`

## Usage

```scala
addSbtPlugin("com.github.sbt" % "sbt-matrix-sources" % "<version>")
```

```scala
lazy val core = (projectMatrix in file("core"))
  .jvmPlatform(scalaVersions = Seq("2.13.18", "3.9.0"))
  .jsPlatform(scalaVersions = Seq("3.9.0"))
```

For the JVM row, the plugin adds these directories under `core/`:

| directory | what to put in it |
|---|---|
| `jvm/src/main/java` | platform, Java |
| `jvm/src/main/{scala*,resources}` | platform, resources, Scala and all prefix versions |
| `shared/src/main/{scala*,resources}` | all platforms, resources and Scala (with versions) |

Note that with only two platforms `shared` covers both, and there are no partial
cross-platform subgroups. If we also use `.nativePlatform(scalaVersions = Seq("3.9.0"))`,
it adds one directory for each pair, so for JVM it would include:

| directory | what to put in it |
|---|---|
| `js-jvm/src/main/{scala*,resources}` | JS and JVM rows (subdirs, as above) |
| `jvm-native/src/main/{scala*,resources}` | JVM and Native rows (subdirs, as above) |

## Requirements

sbt 2, where `ProjectMatrix` is part of sbt itself. In an sbt 1 build, `sbt-projectmatrix`
provides `projectMatrix`, and this plugin does not support that.
