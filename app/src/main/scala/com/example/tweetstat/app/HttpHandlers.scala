package com.example.tweetstat.app

import java.time.Instant

import com.example.tweetstat.AlgebirdStats
import io.circe._
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s._

import scalaz.concurrent.Task
import scalaz.stream.Process

case class HttpHandlers[E, H, D](start: Instant,
                                 stats: Process[Task, AlgebirdStats]) {

  def errorJson(s: String): Json =
    Json.obj("error" -> Json.fromString(s))

  val service = HttpService {
    case GET -> Root =>
      Ok(buildinfo.BuildInfo.toJson)
        .withContentType(Some(MediaType.`application/json`))

    case GET -> Root / "status" =>
      Ok("up")

    case GET -> Root / "stats" / IntVar(k) =>
      import io.circe.syntax._
      for {
        now <- Task.delay(Instant.now)
        result <- stats
          .take(1)
          .map(_.summary(k, finiteDuration(start, now)).asJson)
          .map(Ok(_))
          .runLastOr(NotFound(errorJson("No stats found")))
        result <- result
      } yield result
  }
}
