lazy val check = taskKey[Unit]("")

lazy val plain = project
  .settings(
    // the plugin applies to every project, and adds nothing to one that is no matrix row
    check := Def.uncached {
      val under = (baseDirectory.value.getAbsoluteFile / "src").toPath
      val srcs = (Compile / unmanagedSourceDirectories).value.map(_.getAbsoluteFile)
      assert(srcs.forall(_.toPath.startsWith(under)), s"stray directory in $srcs")
    },
  )
