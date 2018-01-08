object V {
  lazy val circe = sbt.settingKey[String]("")
  lazy val doobie = sbt.settingKey[String]("")
  lazy val scalaz = sbt.settingKey[String]("")
  lazy val scalazStream = sbt.settingKey[String]("")
  lazy val http4s = sbt.settingKey[String]("")
  lazy val journal = sbt.settingKey[String]("")
  lazy val algebird = sbt.settingKey[String]("")
  lazy val logback = sbt.settingKey[String]("")

def http4sFrom(s: String): String = s match {
  case "7.2.+" => "0.16.5a"
}
def scalazStreamFrom(s: String): String = s match {
  case "7.2.+" => "0.8.6a"
}

}
