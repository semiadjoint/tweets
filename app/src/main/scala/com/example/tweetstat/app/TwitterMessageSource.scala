package com.example.tweetstat.app

import com.example.tweetstat.{Err, Message, Url}
import io.circe.{Decoder, HCursor, Json}
import journal.Logger
import org.http4s.client.{Client, oauth1}
import org.http4s.{EntityBody, Method, Request, Uri}

import scalaz.\/
import scalaz.concurrent.Task
import scalaz.stream.Process

object TwitterMessageSource {
  object Config {
    case class Secrets(
      consumerSecret: Secret,
      tokenSecret: Secret)
  }
  case class Config(client: Client,
                    consumerKey: String,
                    consumerSecret: Secret,
                    token: String,
                    tokenSecret: Secret)

  implicit val decodeUrl: Decoder[Url] = new Decoder[Url] {
    final def apply(c: HCursor): Decoder.Result[Url] =
      for {
        displayUrl <- c.downField("display_url").as[String]
      } yield Url(displayUrl)
  }
  implicit val decodeMessage: Decoder[Message] = new Decoder[Message] {
    final def apply(c: HCursor): Decoder.Result[Message] =
      for {
        text <- c.downField("text").as[String]
        urls <- c.downField("entities").downField("urls").as[List[Url]]
      } yield Message(text, urls)
  }
}
case class TwitterMessageSource(config: TwitterMessageSource.Config) {
  val log = Logger[this.type]

  val client = config.client

  val request = Request(
    Method.GET,
    Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))

  def byteStream: EntityBody =
    for {
      sr <- Process.eval(
        oauth1.signRequest(
          request,
          oauth1.Consumer(
            config.consumerKey,
            config.consumerSecret.unsafeExtractClearText
          ),
          None,
          None,
          Some(
            oauth1.Token(config.token,
                         config.tokenSecret.unsafeExtractClearText))
        )
      )
      res <- client.streaming(sr)(resp => resp.body)
    } yield res

  def jsonStream: Process[Task, Json] = {
    implicit val f = io.circe.jawn.CirceSupportParser.facade
    import jawnstreamz._
    byteStream.parseJsonStream
  }

  def messageStream: Process[Task, Err \/ Message] = {
    jsonStream.map { json =>
      json
        .as[Message](TwitterMessageSource.decodeMessage)
        .fold(x => \/.left(Err(s"${x.message}: ${json.toString}")), \/.right)
    }
  }
}
