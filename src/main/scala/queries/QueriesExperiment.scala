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
import skunk.codec.all.bpchar
import cats.syntax.traverse.toTraverseOps
import cats.syntax.all.toFlatMapOps
import natchez.Trace.Implicits.noop

object QueriesExperiment extends IOApp:
  // Mapping Query Results
  final case class Country(
    name: String,
    code: String,
    population: Int,
    headofstate: Option[String],
    capital: Option[Int],
  )

  // Decoder
  val countryDecoder: Decoder[Country] = (varchar *: bpchar(3) *: int4 *: varchar.opt *: int4.opt).map {
    case (n, c, p, hos, capt) => Country(n, c, p, hos, capt)
  }

  // Simple query
  val counriesAndPopulationGenerically: Query[Void, Country] =
    sql"SELECT name, code, population, headofstate, capital FROM country"
      .query(varchar *: bpchar(3) *: int4 *: varchar.opt *: int4.opt)
      .to[Country]

  // An extended query is a query with parameters, or a simple query
  // that is executed via the extended query protocol.
  val countiresLikeName: Query[String, Country] =
    sql"""
      SELECT name, code, population, headofstate, capital
      FROM country
      WHERE name LIKE $varchar
    """.query(countryDecoder)

  // Try to run the extended query via Session#execute, or the simple query via Session#prepare.
  // Note that in the latter case you will need to pass the value Void as an argument.
  // 1. Yes, we can run extended query via Session#execute.
  //    Call of session.execute(extendedQuery) return fuction (A) => F[List[B]]:
  //    val ps: (String) => IO[List[Country]] = session.execute(countiresLikeName)
  //    So, we can just passed argument and then got query result
  def runExtendedQueryViaExecute(session: Session[IO]) =
    for {
      countries <- session.execute(countiresLikeName)("U%")
      _         <- countries.traverse(IO.println)
    } yield ()

  // 2. We can run it like this:
  def runSimpleQueryViaPrepare(session: Session[IO]) =
    for {
      result <- session.prepare(counriesAndPopulationGenerically)
      _      <- result.stream(Void, 32).flatTap(c => Stream.eval(IO.println(c))).compile.drain
    } yield ()

  // Add/remove/change encoders and decoders. Do various things to make the queries fail.
  // Which kinds of errors are detected at compile-time vs. runtime?

  // Add more fields to Country and more colums to the query; or add more parameters.
  // You will need to consult the Schema Types reference to find the encoders/decoders you need.

  // Experiment with the treatment of nullable columns.
  // You need to add .opt to encoders/decoders (int4.opt for example) to indicate nullability.
  // Keep in mind that for interpolated encoders you'll need to write ${int4.opt}.
  val countiresByHeadOfState: Query[Option[String], Country] =
    sql"""
      SELECT name, code, population, headofstate, capital
      FROM country
      WHERE headofstate LIKE ${varchar.opt}
    """.query(countryDecoder)

  def countryByHeadOfState(hosPat: Option[String], session: Session[IO]) =
    for {
      ps <- session.prepare(countiresByHeadOfState)
      _  <- ps.stream(hosPat, 32).flatTap(c => Stream.eval(IO.println(c))).compile.drain
    } yield ()

  private val session: Resource[IO, Session[IO]] = Session.single[IO](
    host = "localhost",
    port = 5432,
    user = "jimmy",
    database = "world",
    password = Some("banana"),
  )

  def run(args: List[String]): IO[ExitCode] =
    session.use { s =>
      countryByHeadOfState(Option("Elisabeth%"), s).as(ExitCode.Success)
    }
