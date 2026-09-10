def repoSlug = "sbt/sbt-matrix-sources"
def repoUri = uri(s"https://github.com/$repoSlug")
def javaSpec = JavaSpec.temurin("17")

def thisBuildSettings = Def.settings(
  organization := "com.github.sbt",
  description := "An sbt plugin to enable sbt-crossproject-compatible layout in projectMatrix",
  homepage := Some(repoUri),
  licenses += ("Apache-2.0", uri("https://www.apache.org/licenses/LICENSE-2.0.html")),
  scmInfo := Some(ScmInfo(repoUri, s"scm:git@github.com:$repoSlug.git")),
  developers := Developer(
    id = "kitbellew",
    name = "kitbellew",
    email = "@kitbellew",
    url = uri("https://github.com/kitbellew"),
  ) :: Nil,
  dynverSonatypeSnapshots := true,
  scalaVersion := "3.8.4", // tied to `pluginCrossBuild / sbtVersion` below

  githubWorkflowBuildMatrixFailFast := Some(false),
  githubWorkflowBuild := Seq( // commands in each step
    List("test"),
    List("scripted"),
    List("scalafmtCheckRepo"),
  ).map(commands => WorkflowStep.Sbt(commands, cond = Some("!cancelled()"))),
  githubWorkflowTargetTags ++= Seq("v**"),
  githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
  githubWorkflowPublish := Seq(WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}",
    ),
  )),
  githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest", "windows-latest"),
  githubWorkflowJavaVersions := Seq(javaSpec),
  githubWorkflowPublishJavaVersion := javaSpec,
)

inThisBuild(thisBuildSettings)

lazy val plugin = project.in(file(".")).enablePlugins(SbtPlugin).settings(
  name := "sbt-matrix-sources",
  pluginCrossBuild / sbtVersion := "2.0.0",
  scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
  scriptedBufferLog := false,
)
