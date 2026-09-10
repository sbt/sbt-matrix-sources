/*
 * sbt-matrix-sources
 * Copyright 2026, the sbt contributors
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package sbtmatrixsources

import sbt.*
import sbt.Keys.*
import sbt.ProjectExtra.inConfig

/**
 * Adds the source directories that `sbt-crossproject` lays out.
 *
 * @see [[https://github.com/sbt/sbt-matrix-sources the readme]], for the directories it adds
 */
object ProjectMatrixSourcesPlugin extends AutoPlugin:

  override def trigger = allRequirements

  private object filters extends ScopeFilter.Make

  private val projectMatrixSourcesRowAxes = settingKey[Option[(File, Seq[VirtualAxis])]](
    "The matrix base directory and the axes of this row, if the project is a matrix row",
  )

  private val projectMatrixSourcesPlatforms =
    settingKey[Seq[String]]("Every platform this project's matrix has a row for")

  override def projectSettings: Seq[Setting[?]] = Def.settings(
    // sbt resolves the base this way, for the row's own sourceDirectory
    projectMatrixSourcesRowAxes :=
      projectMatrixBaseDirectory.?.value.map(_.getAbsoluteFile).zip(virtualAxes.?.value),
    projectMatrixSourcesPlatforms := projectMatrixSourcesRowAxes.value.fold(Seq.empty[String]) {
      case (base, _) => projectMatrixSourcesRowAxes.all(ScopeFilter(filters.inAnyProject)).value
          .flatten.flatMap { case (b, axes) => if b == base then platformOf(axes) else None }
          .distinct.sorted
    },
    inConfig(Compile)(sourceDirectories("main")),
    inConfig(Test)(sourceDirectories("test")),
  )

  private def sourceDirectories(conf: String): Seq[Setting[?]] = Def.settings(
    unmanagedSourceDirectories ++= layouts.value.flatMap { l =>
      val variants = scalaDirectories(l.scalaVersion, crossPaths.value)
      // add Java only to the platform's own directory
      l.dir(l.platform, conf, "java") +: l.sources.flatMap(s => variants.map(l.dir(s, conf, _)))
    },
    unmanagedResourceDirectories ++=
      layouts.value.flatMap(l => l.sources.map(l.dir(_, conf, "resources"))),
  )

  private def layouts = Def.setting(projectMatrixSourcesRowAxes.value match {
    case Some((base, axes)) => platformOf(axes).fold(Seq.empty[Layout]) { platform =>
        // scalaVersion, not value: value is the directory suffix, which a build is free to name
        val version = axes.collectFirst { case a: VirtualAxis.ScalaVersionAxis => a.scalaVersion }
        val sources = platformSources(projectMatrixSourcesPlatforms.value, platform)
        Layout(base, platform, version, sources) :: Nil
      }
    case _ => Nil
  })

  private final class Layout(
      base: File,
      val platform: String,
      val scalaVersion: Option[String],
      val sources: Seq[String],
  ):
    def dir(src: String, conf: String, subdir: String): File = base / src / "src" / conf / subdir

  private def platformOf(axes: Seq[VirtualAxis]): Option[String] = axes
    .collectFirst { case a: VirtualAxis.PlatformAxis => a.value }

  /**
   * The directories to add for a row of `platform`: its own, `shared`, and one for each group of
   * platforms that share code. It leaves out the group of every platform, because `shared` is that
   * group.
   */
  private def platformSources(platforms: Seq[String], platform: String): Seq[String] =
    val others = platforms.filter(_ != platform)
    val subsets = others.foldLeft(Seq(Seq.empty[String]))((acc, p) => acc ++ acc.map(_ :+ p))
    val groups = subsets
      .collect { case s if s.nonEmpty && s.size < others.size => (platform +: s).sorted.mkString("-") }
    platform +: "shared" +: groups

  /** `scala-2.13.18`, then every shorter prefix of it, then `scala`. */
  private def scalaDirectories(version: Option[String], crossPaths: Boolean): List[String] =
    version match
      case Some(v) if crossPaths =>
        val res = List.newBuilder[String]
        var end = v.length
        while end > 0 do
          res += s"scala-${v.substring(0, end)}"
          end = v.lastIndexOf('.', end - 1)
        res += "scala"
        res.result()
      case _ => "scala" :: Nil

end ProjectMatrixSourcesPlugin
