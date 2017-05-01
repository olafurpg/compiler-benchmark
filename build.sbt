name := "compiler-benchmark"

version := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.8"

val dottyVersion = settingKey[String]("Dotty version to be benchmarked.")

dottyVersion in ThisBuild := "0.1.1-bin-20170429-10a2ce6-NIGHTLY"

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

lazy val compilation = project
  .enablePlugins(JmhPlugin)
  .settings(
    ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
    description := "Black box benchmark of the compilers",
    libraryDependencies +=  ScalaArtifacts.Organization % ScalaArtifacts.LibraryID % "2.11.8",
//    libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.11.8",
    libraryDependencies += "ch.epfl.lamp" % "dotty-compiler_2.11" % dottyVersion.value,
    libraryDependencies += "ch.epfl.lamp" % "dotty-library_2.11" % dottyVersion.value
  )

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

runBatchVersions := List(
  "0.1.1-bin-20170429-10a2ce6-NIGHTLY"
)

runBatchBenches := Seq(
  (compilation, "HotDotcBenchmark")
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

commands += Command.args("runBatch", "") { (s: State, args: Seq[String]) =>
  val targetDir = target.value
  val outFile = targetDir / "combined.csv"

  def filenameify(s: String) = s.replaceAll("""[@/:]""", "-")

  val tasks: Seq[State => State] = for {
    p <- runBatchSources.value.map(x => (filenameify(x), s"-p source=$x"))
    (sub, b) <- runBatchBenches.value
    v <- runBatchVersions.value
  } yield {
    import ScalaArtifacts._
    val scalaLibrary = Organization % LibraryID % "2.11.5"
    val dottyLibrary = "ch.epfl.lamp" % "dotty-library_2.11" % v
    val ioArgs = s"-rf csv -rff $targetDir/${p._1}-$b-$v.csv"
    val argLine = s"$b ${p._2} $ioArgs"

    (s1: State) =>
    {
      val s2 = setVersion(s1, sub, v)
      val extracted = Project.extract(s2)
      val (s3, cp) = extracted.runTask(fullClasspath in sub in Jmh, s2)
      val cpFiles = cp.files
      val dottyArt = cpFiles.filter(_.getName.contains(dottyLibrary.name))
      val scalaArt = cpFiles.filter(_.getName.contains(scalaLibrary.name))
      val classpath = (dottyArt ++ scalaArt).mkString(":")
      val cargs = s" -p _classpath=$classpath $argLine -p _dottyVersion=$v"
      val (s4, _) = extracted.runInputTask(run in sub in Jmh, cargs, s3)
      s4
    }
  }
  tasks.foldLeft(s)((state: State, fun: (State => State)) => {
    val newState = fun(state)
    Project
      .extract(newState)
      .runInputTask(runMain in ui in Compile,
        " compilerbenchmark.PlotData",
        newState)
      ._1
  })
}

addCommandAlias("hot", "compilation/jmh:run HotScalacBenchmark")

addCommandAlias("cold", "compilation/jmh:run ColdScalacBenchmark")

addCommandAlias("hot-dotc", "compilation/jmh:run HotDotcBenchmark")

addCommandAlias("cold-dotc", "compilation/jmh:run ColdDotcBenchmark")
