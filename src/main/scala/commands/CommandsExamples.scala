package commands

import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.kernel.Resource
import skunk.syntax.all.sql
import skunk.Command
import skunk.Void
import skunk.Session
import skunk.codec.all.int2
import skunk.codec.all.bpchar
import skunk.codec.all.varchar
import natchez.Trace.Implicits.noop

object CommandsExamples extends IOApp:
  // A command is a SQL statement that does not return rows.

  // Simple Command.
  // Command is parameterized by its input type.
  // Because this command has no parameters the input type is Void.
  // A simple command is a command with no parameters.
  val simpleCommand: Command[Void] = sql"SET SEED TO 0.123".command

  // Parameterized Command
  val parameterizedCommand: Command[String] =
    sql"DELETE FROM country WHERE name = $varchar".command

  // Contramapping Command
  final case class Info(code: String, hos: String)
  val updateCommand: Command[Info] =
    sql"""
      UPDATE country
      SET headofstate = $varchar
      WHERE code = ${bpchar(3)}
    """.command
      .contramap { case Info(code, hos) => code *: hos *: EmptyTuple }

  val updateCommandTo: Command[Info] =
    sql"""
      UPDATE country
      SET headofstate = $varchar
      WHERE code = ${bpchar(3)}
    """.command.to[Info]

  // List Parameters
  def deleteMany(n: Int): Command[List[String]] =
    sql"DELETE FROM country WHERE name IN (${varchar.list(n)})".command

  val delete3 = deleteMany(3)

  def insertMany(n: Int): Command[List[(String, Short)]] =
    val encoder = (varchar *: int2).values.list(n)
    sql"INSERT INTO pets VALUES $encoder".command

  val insert3 = insertMany(3)

  def insertExactly(xs: List[(String, Short)]): Command[xs.type] =
    val enocder = (varchar *: int2).values.list(xs)
    sql"INSERT INTO pets VALUES $enocder".command

  val pairs = List[(String, Short)](("Bob", 3), ("Alice", 7))

  val insertPairs = insertExactly(pairs)

  private val session: Resource[IO, Session[IO]] = Session.single[IO](
    host = "localhost",
    port = 5432,
    database = "world",
    user = "jimmy",
    password = Some("banana")
  )

  def run(args: List[String]): IO[ExitCode] =
    session.use { s =>
      s.execute(simpleCommand).flatTap(IO.println).as(ExitCode.Success)
      s.prepare(insertPairs).flatMap { pc => pc.execute(pairs) }.as(ExitCode.Success)
      // We got type mismatch error:
      // Found:    List[(String, Short)]
      // Required: (commands.CommandsExamples.pairs : List[(String, Short)])
      // s.prepare(insertPairs).flatMap { pc => pc.execute(pairs.drop(1)) }.as(ExitCode.Success)
    }
