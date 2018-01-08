libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
)
libraryDependencies ++= Seq(
  "io.verizon.journal" %% "core" % V.journal.value,
  "ch.qos.logback" % "logback-classic" % V.logback.value
)

libraryDependencies += "com.twitter" %% "algebird-core" % V.algebird.value

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % V.circe.value)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core",
  "org.scalaz" %% "scalaz-concurrent"
).map(_ % V.scalaz.value)

libraryDependencies += "org.scalaz.stream" %% "scalaz-stream" % V.scalazStream.value
