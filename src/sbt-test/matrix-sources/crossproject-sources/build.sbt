lazy val check = taskKey[Unit]("")

// `.` in the base, to tell apart the ways the source directories resolve it
lazy val core = (projectMatrix in file("./core"))
  .settings(
    // Def.uncached: Seq[VirtualAxis] has no HashWriter, so the task cannot be cached
    check := Def.uncached {
      val base = projectMatrixBaseDirectory.value.getAbsoluteFile
      def dir(parts: String*): File = parts.foldLeft(base)(_ / _)
      val srcs = (Compile / unmanagedSourceDirectories).value.map(_.getAbsoluteFile).toSet
      // the row's Scala version, then every shorter prefix of it, then `scala`
      val axis = virtualAxes.value.collectFirst { case a: VirtualAxis.ScalaVersionAxis => a.value }
      val variants = axis.get match {
        case "2.13" => Set("scala-2.13.18", "scala-2.13", "scala-2", "scala")
        case "3"    => Set("scala-3.9.0", "scala-3.9", "scala-3", "scala")
        case "3.3"  => Set("scala-3.3.8", "scala-3.3", "scala-3", "scala")
        // a full version, as `CrossVersion.full` and semanticdb use
        case "2.13.17" => Set("scala-2.13.17", "scala-2.13", "scala-2", "scala")
        case "3_7"     => Set("scala-3.7.3", "scala-3.7", "scala-3", "scala")
      }
      // shared, the row's own platform, and each group short of every platform
      val dirs = Seq("shared", "jvm", "js-jvm", "jvm-native")
      val wanted = dirs.flatMap(d => variants.map(v => Seq(d, "src", "main", v))) ++ Seq(
        // the plugin adds java only under the row's own platform
        Seq("jvm", "src", "main", "java"),
      )
      wanted.foreach(p => assert(srcs(dir(p*)), s"missing ${dir(p*)} in $srcs"))
      // the default layout
      val defaults = Seq(Seq("src", "main", "scala"), Seq("src", "main", "scalajvm"))
      defaults.foreach(p => assert(srcs(dir(p*)), s"missing ${dir(p*)} in $srcs"))
      val sharedMain = dir("shared", "src", "main")
      val shared = srcs.filter(_.getParentFile == sharedMain).map(_.getName)
      assert(shared == variants, s"$shared under $sharedMain")
      // the group of every platform is what shared is
      assert(!srcs(dir("js-jvm-native", "src", "main", "scala")), "no all-platform group")
      val res = (Compile / unmanagedResourceDirectories).value.map(_.getAbsoluteFile).toSet
      dirs.map(Seq(_, "src", "main", "resources"))
        .foreach(p => assert(res(dir(p*)), s"missing ${dir(p*)} in $res"))
      val tests = (Test / unmanagedSourceDirectories).value.map(_.getAbsoluteFile).toSet
      assert(tests(dir("shared", "src", "test", "scala")), s"missing test directory in $tests")
    },
  )
  .jvmPlatform(scalaVersions = Seq("2.13.18", "3.9.0"))
  // a row whose axis names the minor version, which `jvmPlatform` never builds
  .customRow(true, Seq(VirtualAxis.jvm, VirtualAxis.scalaPartialVersion("3.3.8")), identity[Project])
  .customRow(
    true,
    Seq(VirtualAxis.jvm, VirtualAxis.scalaVersionAxis("2.13.17", "2.13.17")),
    identity[Project],
  )
  // a row whose axis is named for a project id, as a build that wants no dot in it writes
  .customRow(
    true,
    Seq(VirtualAxis.jvm, VirtualAxis.ScalaVersionAxis("3.7.3", "3_7")),
    identity[Project],
  )
  /*
   * js and native rows, so the groups the JVM row shares with them exist. Neither platform
   * plugin is needed to name a directory.
   */
  .customRow(true, Seq("3.9.0"), Seq(VirtualAxis.js), _.settings(platform := "sjs1"))
  .customRow(true, Seq("3.9.0"), Seq(VirtualAxis.native), _.settings(platform := "native0.5"))
