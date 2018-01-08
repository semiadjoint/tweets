package com.example.tweetstat

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit

import journal.Logger

import scala.concurrent.duration.FiniteDuration
import scalaz.concurrent.Task
import scalaz.stream.Process

package object app {
  val log = Logger[this.type]

  implicit class ProcessOps[A](p: Process[Task, A]) {
    def logged(log: String => Unit): Process[Task, A] = {
      val sink: Process[Task, A => Task[Unit]] =
        Process.constant((x: A) => Task.delay(log(x.toString)))
      p.observe(sink)
    }
  }
  def finiteDuration(start: Instant, end: Instant): FiniteDuration =
    FiniteDuration(Duration.between(start, end).toNanos, TimeUnit.NANOSECONDS)


}
