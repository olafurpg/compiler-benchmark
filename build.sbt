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

lazy val DottyRef = ".*-([^-]+)-NIGHTLY".r

lazy val batchTasks = taskKey[List[String]]("")
batchTasks := tasks
lazy val tasks = {
  val dottyLatestVersion @ DottyRef(dottyRef) = dottyLatestNightlyBuild.get
  for {
    (compiler, sourceDirectory, version, ref) <- List(
      ("Dotc", "dotty", dottyLatestVersion, dottyRef),
      ("Scalac", "scala", "2.11.8", "18f625db1c")
    )
    scalaVersion = s" -DscalaVersion=$version"
    baseSourceDir = sys.props.getOrElse("gitrepos", "/home/benchs")
    scalaRef = s" -DscalaRef=$ref"
    localdir = s" -Dgit.localdir=$baseSourceDir$sourceDirectory"
    sysProps = s"$scalaVersion $scalaRef $localdir"
    runUpload = s"compilation/jmh:runMain $sysProps scala.bench.UploadingRunner "
    inputProject <- List("vector", "squants")
    source = s"-p source=$inputProject"
    bench = s"(Cold|Warm|Hot)${compiler}Benchmark"
  } yield s"""; clean ; $runUpload $bench $source """.stripMargin
}

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
commands += runBoth("cold")
commands += runBoth("warm")
commands += runBoth("hot")
def runBoth(cmd: String) = Command.args(cmd, cmd) {
  case (state, args) =>
    s"$cmd-dotc " + args.mkString(" ") ::
      s"$cmd-scalac " + args.mkString(" ") ::
      state
}
