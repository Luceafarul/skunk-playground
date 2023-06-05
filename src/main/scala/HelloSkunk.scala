package skunk.playground

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.effect.ExitCode
import skunk.Session
import skunk.codec.all.date
import skunk.syntax.all.sql
import natchez.Trace.Implicits.noop

object HelloSkunk extends IOApp:
  val session: Resource[IO, Session[IO]] = Session.single(
    host = "localhost",
    port = 5432,
    user = "jimmy",
    database = "world",
    password = Some("banana"),
  )

  def run(args: List[String]): IO[ExitCode] =
    session.use { s =>
      for {
        d <- s.unique(sql"select current_date".query(date))
        _ <- IO.println(s"The current date is $d")
      } yield ExitCode.Success
    }
