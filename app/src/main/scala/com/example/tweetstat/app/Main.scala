package com.example.tweetstat.app

import journal.Logger
import org.http4s.util.ProcessApp

import scalaz.concurrent.Task
import scalaz.stream.Process

object Main extends ProcessApp {
  val log = Logger[this.type]

  val config: App.Config = AppConfig.load.unsafePerformSyncAttempt.fold(
    t => {
      log.error(s"Loading config failed: ${t.getMessage}")
      throw t
    },
    identity
  )

  val app = App(config)

  override def process(args: List[String]): Process[Task, Nothing] = {
    Process
      .eval(app.startBackgroundJobs(app.atomicStats))
      .flatMap(app.startHttpServer)
  }
}
