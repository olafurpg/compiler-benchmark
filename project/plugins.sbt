// Comment to get more information during initialization
logLevel := Level.Warn

// sbt-jmh plugin - pulls in JMH dependencies too
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.16")

addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.1.0-RC2")
