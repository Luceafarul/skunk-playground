package commands.service

import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.traverse.toTraverseOps
import skunk.Session
import skunk.syntax.all.sql
import natchez.Trace.Implicits.noop

object App extends IOApp:
  // A source of sessions
  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      user = "jimmy",
      database = "world",
      password = Some("banana"),
    )

  // A resources that creates and drops a temporary table
  def withPetsTable(session: Session[IO]): Resource[IO, Unit] =
    val acquire = session.execute(sql"CREATE TEMP TABLE pets (name varchar, age int4)".command).void
    val release = session.execute(sql"DELETE FROM pets".command).void
    Resource.make(acquire)(_ => release)

  // Sample data
  val bob     = Pet("Bob", 12)
  val beagles = List(Pet("John", 2), Pet("George", 3), Pet("Paul", 6), Pet("Ringo", 3))

  // Entry point
  def run(args: List[String]): IO[ExitCode] =
    session.flatTap(s => withPetsTable(s)).map(s => PetService.fromSession(s)).use { service =>
      for {
        _    <- service.insert(bob)
        _    <- service.insert(beagles)
        pets <- service.selectAll
        _    <- pets.traverse(pet => IO.println(pet))
      } yield ExitCode.Success
    }
