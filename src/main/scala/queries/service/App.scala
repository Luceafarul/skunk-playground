package queries.service

import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.kernel.Resource
import skunk.Session
import natchez.Trace.Implicits.noop

object App extends IOApp:
  // A source of sessions
  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      user = "jimmy",
      database = "world",
      password = Some("banana")
    )

  // A source of services
  val service: Resource[IO, Service[IO]] =
    session.evalMap(s => Service.fromSession(s))

  // Entry point ...
  // There is no indication that we're using a database at all!
  def run(args: List[String]): IO[ExitCode] =
    service.use { s =>
      for {
        ts <- s.currentTimestamp
        _  <- IO.println(s"The current timestamp is $ts")
        _ <- s
          .countriesByName("U%")
          .evalMap(c => IO.println(c))
          .compile
          .drain
      } yield ExitCode.Success
    }
