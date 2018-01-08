lazy val root = project
  .in(file("."))
  .dependsOn(app)
  .aggregate(core, app)
  .settings(aggregate.in(reStart) := false)
  .settings(
    V.circe in ThisBuild := "0.8.0",
    V.doobie in ThisBuild := "0.4.4",
    V.scalaz in ThisBuild := "7.2.+",
    V.scalazStream in ThisBuild := V.scalazStreamFrom(V.scalaz.value),
    V.journal in ThisBuild := "3.0.19",
    V.logback in ThisBuild := "1.2.3",
    V.algebird in ThisBuild := "0.13.3",
    V.http4s in ThisBuild := V.http4sFrom(V.scalaz.value)
  )
  .settings(
    mainClass in Global := Some("com.example.tweetstat.app.Main")
  )
  .settings(
    // Turn off compiler crankiness:
    // scalacOptions.in(core, Test) ~= filterConsoleScalacOptions
    // scalacOptions.in(app, Test) ~= filterConsoleScalacOptions
  )
  .settings(
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.5"),
    addCommandAlias("build", ";test:compile"),
    addCommandAlias("rebuild", ";reload;build"),
    addCommandAlias("retest", ";reload;test"),
    organization := "com.example",
    name := "tweetstat",
    version := "0.0.1-SNAPSHOT",
    scalaVersion in ThisBuild := "2.12.4",
    scalafmtOnCompile := true
  )

lazy val core = project

lazy val app = project
  .dependsOn(core % "test->test;compile->compile")
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "buildinfo",
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson
  )
