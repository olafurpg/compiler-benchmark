name := "compiler-benchmark"

version := "1.0-SNAPSHOT"

val dottyVersion = settingKey[String]("Dotty version to be benchmarked.")

dottyVersion in ThisBuild := sys.env.getOrElse("DOTTY_VERSION",
                                               DottyVersion.latestNightly.get)

scalaVersion in ThisBuild := "2.11.8"

// Convenient access to builds from PR validation
resolvers ++= (
  if (scalaVersion.value.endsWith("-SNAPSHOT"))
    List(
      "pr-scala snapshots old" at "http://private-repo.typesafe.com/typesafe/scala-pr-validation-snapshots/",
      "pr-scala snapshots" at "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/",
      Resolver.mavenLocal,
      Resolver.sonatypeRepo("snapshots")
    )
  else
    Nil
)

lazy val infrastructure = project
  .enablePlugins(JmhPlugin)
  .settings(
    description := "Infrastrucuture to persist benchmark results annotated with metadata from Git",
    autoScalaLibrary := false,
    crossPaths := false,
    libraryDependencies ++= Seq(
      "org.influxdb" % "influxdb-java" % "2.5", // TODO update to 2.6 when released for fix for https://github.com/influxdata/influxdb-java/issues/269
      "org.eclipse.jgit" % "org.eclipse.jgit" % "4.6.0.201612231935-r",
      "com.google.guava" % "guava" % "21.0",
      "org.apache.commons" % "commons-lang3" % "3.5",
      "com.typesafe" % "config" % "1.3.1",
      "org.slf4j" % "slf4j-api" % "1.7.24",
      "org.slf4j" % "log4j-over-slf4j" % "1.7.24",
      "ch.qos.logback" % "logback-classic" % "1.2.1"
    )
  )

lazy val dotcRuntime = project
  .in(file("dotc-runtime"))
  .settings(
    scalaVersion := dottyVersion.value,
    libraryDependencies += "ch.epfl.lamp" % "dotty-compiler_0.1" % dottyVersion.value,
    libraryDependencies += "ch.epfl.lamp" % "dotty-library_0.1" % dottyVersion.value
  )

lazy val scalacRuntime = project
  .in(file("scalac-runtime"))
  .settings(
    libraryDependencies += ScalaArtifacts.Organization % ScalaArtifacts.CompilerID % "2.11.8",
    libraryDependencies += ScalaArtifacts.Organization % ScalaArtifacts.LibraryID % "2.11.8"
  )

lazy val compilation = project
  .enablePlugins(JmhPlugin)
  .settings(
    ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
    description := "Black box benchmark of the compilers",
    fork in run := true,
    buildInfoKeys := Seq[BuildInfoKey](
      dottyVersion,
      BuildInfoKey.map(fullClasspath.in(dotcRuntime, Compile)) {
        case (_, cp) =>
          val dottyClasspath = cp.map(_.data.getAbsolutePath)
          "dotcClasspath" -> dottyClasspath
      },
      BuildInfoKey.map(fullClasspath.in(scalacRuntime, Compile)) {
        case (_, cp) =>
          val scalacClasspath = cp.map(_.data.getAbsolutePath)
          "scalacClasspath" -> scalacClasspath
      }
    )
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(infrastructure)

lazy val micro = project
  .enablePlugins(JmhPlugin)
  .settings(
    description := "Finer grained benchmarks of compiler internals",
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
  )

lazy val jvm = project
  .enablePlugins(JmhPlugin)
  .settings(
    description := "Pure Java benchmarks for demonstrating performance anomalies independent from the Scala language/library",
    autoScalaLibrary := false,
    crossPaths := false
  )

val ui = project.settings(
  scalaVersion := "2.11.8",
  libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.3",
  libraryDependencies += "com.h2database" % "h2" % "1.4.192"
)

val runBatch = taskKey[Unit]("Run a batch of benchmark suites")
val runBatchVersions = settingKey[Seq[String]]("Compiler versions")
val runBatchBenches = settingKey[Seq[(sbt.Project, String)]]("Benchmarks")
val runBatchSources = settingKey[Seq[String]]("Sources")

runBatchVersions :=
  List(
    "0.1.1-bin-20170501-b19d1fb-NIGHTLY",
    "0.1.1-bin-20170501-de53e52-NIGHTLY",
    "0.1.1-bin-20170502-df22149-NIGHTLY",
    "0.1.1-bin-20170504-92fe2a5-NIGHTLY",
    "0.1.1-bin-20170506-ea9643c-NIGHTLY",
    "0.1.1-bin-20170506-385178d-NIGHTLY",
    "0.1.1-bin-20170507-1014af3-NIGHTLY",
    "0.1.1-bin-20170508-a391a58-NIGHTLY",
    "0.1.1-bin-20170509-7a3f880-NIGHTLY",
    "0.1.1-bin-20170510-85d9684-NIGHTLY"
  )

runBatchBenches := Seq(
  (compilation, "HotDotcBenchmark"),
  (compilation, "WarmDotcBenchmark"),
  (compilation, "ColdDotcBenchmark")
)

runBatchSources := List(
  // "scalap",
  // "better-files",
  // "squants"
  "vector"
)

def setVersion(s: State, proj: sbt.Project, newVersion: String): State = {
  val extracted = Project.extract(s)
  import extracted._
  if (get(dottyVersion in proj) == newVersion) s
  else {
    val append = Load.transformSettings(
      Load.projectScope(currentRef),
      currentRef.build,
      rootProject,
      (dottyVersion in proj := newVersion) :: Nil)
    val newSession = session.appendSettings(append map (a => (a, Nil)))
    s.log.info(s"Switching to Scala version $newVersion")
    BuiltinCommands.reapply(newSession, structure, s)
  }
}
lazy val ScalaRef = ".*-([^-]+)-NIGHTLY".r

lazy val tasks = for {
  version <- List(
    "0.1.1-bin-20170501-b19d1fb-NIGHTLY",
    "0.1.1-bin-20170501-de53e52-NIGHTLY",
    "0.1.1-bin-20170502-df22149-NIGHTLY",
    "0.1.1-bin-20170504-92fe2a5-NIGHTLY",
    "0.1.1-bin-20170506-ea9643c-NIGHTLY",
    "0.1.1-bin-20170506-385178d-NIGHTLY",
    "0.1.1-bin-20170507-1014af3-NIGHTLY",
    "0.1.1-bin-20170508-a391a58-NIGHTLY",
    "0.1.1-bin-20170509-7a3f880-NIGHTLY",
    "0.1.1-bin-20170510-85d9684-NIGHTLY"
  )
  setVersion = s"""set dottyVersion in ThisBuild := "$version"  """
  ScalaRef(ref) = version
  sysProps = s"-DscalaVersion=$version -DscalaRef=$ref"
  runUpload = s"compilation/jmh:runMain $sysProps scala.bench.UploadingRunner "
  inputProject <- List("vector")
  source = s"-p source=$inputProject"
  kind <- List("Cold", "Warm", "Hot")
  bench = s"${kind}DotcBenchmark"
} yield s"""; $setVersion ; $runUpload $bench $source """.stripMargin

commands += Command.command("runBatch") { s =>
  tasks.foldLeft(s) {
    case (state, task) =>
      println(s"Running command: $task")
      task :: state
  }
}

addCommandAlias("hot-scalac", "compilation/jmh:run HotScalacBenchmark")

addCommandAlias("cold-scalac", "compilation/jmh:run ColdScalacBenchmark")

addCommandAlias("warm-scalac", "compilation/jmh:run WarmScalacBenchmark")

addCommandAlias("hot-dotc", "compilation/jmh:run HotDotcBenchmark")

addCommandAlias("cold-dotc", "compilation/jmh:run ColdDotcBenchmark")

addCommandAlias("warm-dotc", "compilation/jmh:run WarmDotcBenchmark")

def runBoth(cmd: String) = Command.args(cmd, cmd) {
  case (state, args) =>
    s"$cmd-dotc " + args.mkString(" ") ::
      s"$cmd-scalac " + args.mkString(" ") ::
      state

}

commands += runBoth("cold")
commands += runBoth("warm")
commands += runBoth("hot")
