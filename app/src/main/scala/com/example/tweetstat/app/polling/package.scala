package com.example.tweetstat.app

import java.time.Instant

import com.example.tweetstat.AlgebirdStats

import scalaz.Reader
import scalaz.concurrent.Task
import scalaz.stream.async.mutable.Signal
import scalaz.stream.{time, Process}

package object polling {

  /* This process periodically polls the above-mentioned stats signal,
   * according to the cadence specified in the config. This produces a
   * stream of stats summaries, with lossy-histogram size as specified in
   * the config.
   * */
  def pollStats(start: Instant, atomicStats: Signal[AlgebirdStats])
  : Reader[App.Config, Process[Task, AlgebirdStats.Summary]] = Reader { config =>
    for {
      _ <- time.awakeEvery(config.pollerCadence)(Threads.schedulingExecutor,
        Threads.schedulingPool)
      stats <- Process.eval(atomicStats.get)
      now <- Process.eval(Task.delay(Instant.now))
      summary = stats.summary(config.pollerHistogramSize,
        finiteDuration(start, now))
    } yield summary
  }

}
