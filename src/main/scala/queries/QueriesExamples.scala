package queries

import cats.effect.Resource
import cats.effect.ExitCode
import cats.effect.IOApp
import cats.effect.IO
import fs2.Stream
import skunk.Session
import skunk.Decoder
import skunk.Query
import skunk.Void
import skunk.~
import skunk.data.Type
import skunk.syntax.all.sql
import skunk.codec.all.int4
import skunk.codec.all.varchar
import cats.syntax.traverse.toTraverseOps
import natchez.Trace.Implicits.noop

object QueriesExamples extends IOApp:
  // A query is a SQL statement that can return rows.

  // Single-Column Query
  // A simple query is a query with no parameters.
  val allCountries: Query[Void, String] =
    sql"SELECT name FROM country".query(varchar)

  // Multi-Column Query
  val counriesAndPopulation: Query[Void, String ~ Int] =
    sql"SELECT name, population FROM country".query(varchar ~ int4)

  // Mapping Query Results
  final case class Country(name: String, population: Int)
  val counriesAndPopulationMapped: Query[Void, Country] =
    sql"SELECT name, population FROM country"
      .query(varchar ~ int4)
      .map { case (n, p) => Country(n, p) }

  // Mapping Decoder Results
  val countryDecoder: Decoder[Country] = (varchar ~ int4).map { case (n, p) => Country(n, p) }
  val counriesAndPopulationDecoder: Query[Void, Country] =
    sql"SELECT name, population FROM country".query(countryDecoder)

  // Mapping Decoder Results Generically
  val counriesAndPopulationGenerically: Query[Void, Country] =
    sql"SELECT name, population FROM country"
      .query(varchar *: int4)
      .to[Country]

  // Parameterized Query
  // This is extended query.
  // An extended query is a query with parameters, or a simple query
  // that is executed via the extended query protocol.
  val countiresLikeName: Query[String, Country] =
    sql"""
      SELECT name, population
      FROM country
      WHERE name LIKE $varchar
    """.query(countryDecoder)

  // Multi-Parameter Query
  val countiresLikeNameAndPopulation: Query[String *: Int *: EmptyTuple, Country] =
    sql"""
      SELECT name, population
      FROM country
      WHERE name LIKE $varchar
      AND population > $int4
    """.query(countryDecoder)

  private val session: Resource[IO, Session[IO]] = Session.single[IO](
    host = "localhost",
    port = 5432,
    user = "jimmy",
    database = "world",
    password = Some("banana"),
  )

  def run(args: List[String]): IO[ExitCode] =
    session.use { s =>
      for {
        result <- s.execute(counriesAndPopulationGenerically)
        _      <- result.traverse(IO.println)
      } yield ExitCode.Success
    }

    session.use { s =>
      val stream =
        for {
          ps <- Stream.eval(s.prepare(countiresLikeNameAndPopulation))
          c  <- ps.stream(("U%", 20000000), 64)
          _  <- Stream.eval(IO.println(c))
        } yield ()

      stream.compile.drain.as(ExitCode.Success)
    }
