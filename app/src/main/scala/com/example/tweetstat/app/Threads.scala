package com.example.tweetstat.app

import java.util.concurrent.{
  ExecutorService,
  Executors,
  ScheduledExecutorService,
  ThreadFactory
}

import journal.Logger

import scalaz.concurrent.{Strategy, Task}

object Threads {
  private val log = Logger[this.type]

  def defaultExecutor: Strategy = Strategy.Executor(defaultPool)
  def schedulingExecutor: Strategy = Strategy.Executor(schedulingPool)

  def daemonThreads(name: String) = new ThreadFactory {
    def newThread(r: Runnable) = {
      val t = Executors.defaultThreadFactory.newThread(r)
      t.setDaemon(true)
      t.setName(name)
      t
    }
  }

  def defaultPool: ExecutorService =
    Executors.newFixedThreadPool(4, daemonThreads("continuous-tasks"))

  def schedulingPool: ScheduledExecutorService =
    Executors.newScheduledThreadPool(4, daemonThreads("scheduled-tasks"))

  def runBackground[A](name: String, t: Task[A])(ex: ExecutorService): Unit = {
    log.info(s"Starting the ${name} processor")
    Task
      .fork(t)(ex)
      .unsafePerformAsync(_.fold(
        e => log.error(s"Fatal error in background process: name=${name}", e),
        _ =>
          log.warn(
            s"Background process completed unexpectedly without exception: name=${name}")
      ))
  }


}
