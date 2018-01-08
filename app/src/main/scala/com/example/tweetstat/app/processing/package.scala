package com.example.tweetstat.app

import com.example.tweetstat._

import scalaz.{Reader, \/}
import scalaz.concurrent.Task
import scalaz.stream.async.mutable.Signal
import scalaz.stream.{Sink, merge, Process}

package object processing {
  //todo these don't need the full config. might even be better as a class.

  /* This process pulls tweets from Twitter, analyzes each message
   * and produces a stream of diffs based on message analysis. Those
   * diffs are then used to update the above-mentioned stats signal.
   * */
  def processMessages(atomicStats: Signal[AlgebirdStats]): Reader[App.Config, Process[Task, StatsDiff]] = Reader { config =>
    val messages = TwitterMessageSource(config.twitter).messageStream
    val pps: Process[Task, Process[Task, StatsDiff]] =
      messages.map(x => processSingle(atomicStats)(x).run(config))

    merge.mergeN(pps)
  }

  def processSingle(atomicStats: Signal[AlgebirdStats])(message: Err \/ Message): Reader[App.Config,Process[Task, StatsDiff]] = Reader { config =>
    val analysis = MessageAnalysis(config.analysis, config.stats)
    val handleMessageError: Sink[Task, Err] = Process.constant((e: Err) =>
      Task.delay(log.debug(s"Non-message event received: ${e.getMessage}")))
    Process.eval(Task.now(message))
      .observeW(handleMessageError)
      .stripW
      .map(analysis.diff)
      .observe(analysis.sink(atomicStats))
      .logged(x => log.debug(x.toString))
  }


}
