package queries

import fs2.Stream
import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.kernel.Resource
import skunk.Session
import skunk.Query
import skunk.Void
import skunk.syntax.all.sql
import skunk.codec.all.text
import skunk.codec.all.int4
import skunk.codec.all.bpchar
import skunk.codec.all.varchar
import skunk.codec.all.timestamptz
import java.time.OffsetDateTime
import natchez.Trace.Implicits.noop

object FullQeuryExample extends IOApp:
  // A source of sessions
  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      user = "jimmy",
      database = "world",
      password = Some("banana"),
    )

  // A data model
  final case class Country(name: String, code: String, population: Int)

  // A simple query
  val simpeQuery: Query[Void, OffsetDateTime] =
    sql"SELECT CURRENT_TIMESTAMP".query(timestamptz)

  // An extended query
  val extendedQuery: Query[String, Country] =
    sql"""
      SELECT name, code, population
      FROM country
      WHERE name LIKE $text
    """
      .query(varchar *: bpchar(3) *: int4)
      .to[Country]

  // Run simple query
  def executeSimple(s: Session[IO]): IO[Unit] =
    for {
      ts <- s.unique(simpeQuery)
      _  <- IO.println(s"Current timestamp: $ts")
    } yield ()

  def executeExtended(s: Session[IO]): IO[Unit] =
    val stream: Stream[IO, Unit] = for {
      ps <- Stream.eval(s.prepare(extendedQuery))
      c  <- ps.stream("U%", 64)
      _  <- Stream.eval(IO.println(c))
    } yield ()

    stream.compile.drain

  def run(args: List[String]): IO[ExitCode] =
    session.use { s =>
      for {
        _ <- executeSimple(s)
        _ <- executeExtended(s)
      } yield ExitCode.Success
    }
