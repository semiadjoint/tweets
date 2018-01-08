package com.example.tweetstat.app

import com.example.tweetstat.{AlgebirdStats, Err, MessageAnalysis}
import io.circe
import io.circe.{Decoder, HCursor, parser}
import journal.Logger
import org.http4s.client.blaze.PooledHttp1Client

import scala.concurrent.duration.{FiniteDuration}
import scala.io.Source
import scalaz.concurrent.Task
import scalaz.{\/, stream}
import knobs.{ClassPathResource, Required}

object AppConfig {
  val log = Logger[this.type]

  /* The following task loads our secrets from the specified file.
   * The loaded `knobs.Config` contains our cleartext secrets.
   * To find use sites where secrets are again printable,
   * search for uses of the `unsafeExtractClearText` method.
   * */
  def loadTwitterSecrets(
      path: String): Task[TwitterMessageSource.Config.Secrets] =
    for {
      secrets <- knobs.loadImmutable(List(Required(ClassPathResource(path))))
      result = TwitterMessageSource.Config.Secrets(
        tokenSecret = Secret(secrets.require[String]("twitter.tokenSecret")),
        consumerSecret =
          Secret(secrets.require[String]("twitter.consumerSecret"))
      )
    } yield result

  def load: Task[App.Config] =
    for {
      defaultsCfg <- knobs.loadImmutable(
        List(Required(ClassPathResource("defaults.cfg"))))
      twitterSecrets <- loadTwitterSecrets(
        defaultsCfg.require[String]("twitter.secrets-file"))

      emojiDathFile = defaultsCfg.require[String]("message-analysis.data-file")
      emojiData <- loadEmojiData(emojiDathFile).map(_.fold(throw _, identity))

      client <- Task.delay(PooledHttp1Client())
    } yield
      App.Config(
        serverPort = defaultsCfg.require[Int]("server.port"),
        twitter = TwitterMessageSource.Config(
          client = client,
          consumerKey = defaultsCfg.require[String]("twitter.consumer-key"),
          consumerSecret = twitterSecrets.consumerSecret,
          token = defaultsCfg.require[String]("twitter.token"),
          tokenSecret = twitterSecrets.tokenSecret
        ),
        analysis = MessageAnalysis.Config(
          emojiData = emojiData,
          picDomains = defaultsCfg.require[List[String]]("message-analysis.pic-domains")
        ),
        stats = AlgebirdStats.Config(
          defaultsCfg.require[Int]("stats.histogram-max-counters")
        ),
        pollerCadence = defaultsCfg.require[FiniteDuration]("poller.cadence"),
        pollerHistogramSize = defaultsCfg.require[Int]("poller.top-k-size")
      )

  implicit val decodeMessage: Decoder[(String, String)] =
    new Decoder[(String, String)] {
      final def apply(c: HCursor): Decoder.Result[(String, String)] = {
        for {
          unicodeHex <- c.downField("unified").as[String]
          name <- Right(c.downField("name").as[String].getOrElse(unicodeHex))
        } yield unicodeHex -> name
      }

    }

  def loadEmojiData(path: String): Task[Err \/ Map[String, String]] = {
    val load = stream.io.linesR(Source.fromResource(path)).attempt().map {
      (txt: Throwable \/ String) =>
        for {
          txt <- txt.leftMap(x => Err(x.getMessage))
          x <- parser
            .parse(txt)
            .fold(\/.left, \/.right)
            .leftMap((x: circe.ParsingFailure) => Err(x.message))
          res <- x
            .as[List[(String, String)]]
            .fold(\/.left, \/.right)
            .leftMap((x: circe.Error) =>
              Err("Could not decode emoji json to List"))
        } yield res.toMap
    }
    load.runLastOr(\/.left(Err("Could not load emoji data")))
  }

}
