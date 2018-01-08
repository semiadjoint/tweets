package com.example.tweetstat.app

import java.time.Instant

import com.example.tweetstat._
import journal.Logger
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.duration.FiniteDuration
import scalaz.concurrent.Task
import scalaz.stream.async.mutable.Signal
import scalaz.stream.{Process, async}

object App {
  case class Config(serverPort: Int,
                    twitter: TwitterMessageSource.Config,
                    analysis: MessageAnalysis.Config,
                    stats: AlgebirdStats.Config,
                    pollerCadence: FiniteDuration,
                    pollerHistogramSize: Int)

}

case class App(config: App.Config) {
  val log = Logger[this.type]

  /* This signal gets updated asynchronously in a background task,
   * and exposes a stream of stats that can be polled by a periodic
   * logging task or by HTTP requests. See below for examples of each.
   * */
  val atomicStats: Signal[AlgebirdStats] =
    async.signalOf(AlgebirdStats.empty(config.stats.maxCounters))


  /* Start background job that pulls tweets and updates stats. */
  def startMessageProcessing(atomicStats: Signal[AlgebirdStats]): Task[Unit] =
    Task.delay(
      Threads.runBackground(
        "message-fetch-and-analysis",
        processing.processMessages(atomicStats).run(config).run)(Threads.defaultPool))

  /* Start background job to periodically log stats. */
  def startStatPolling(start: Instant,
                       atomicStats: Signal[AlgebirdStats]): Task[Unit] =
    Task.delay(
      Threads.runBackground("stats-polling",
                            polling.pollStats(start, atomicStats).run(config)
                              .logged(s => log.info(s))
                              .run)(Threads.defaultPool))

  /* Start puller/analyzer and poller log stats. */
  def startBackgroundJobs(atomicStats: Signal[AlgebirdStats]): Task[Instant] =
    for {
      _ <- startMessageProcessing(atomicStats)
      startTime <- Task.delay(Instant.now())
      _ <- startStatPolling(startTime, atomicStats)
    } yield startTime

  def startHttpServer(startTime: Instant): Process[Task, Nothing] = {
    BlazeBuilder
      .bindHttp(config.serverPort)
      .mountService(HttpHandlers(startTime, atomicStats.discrete).service,
                    "/tweetstat/")
      .serve
      .onComplete(Process.eval_(config.twitter.client.shutdown))
  }

}
