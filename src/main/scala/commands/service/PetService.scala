package commands.service

import cats.Monad
import cats.syntax.all.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import skunk.Command
import skunk.Session
import skunk.Query
import skunk.Void
import skunk.syntax.all.sql
import skunk.codec.all.int4
import skunk.codec.all.varchar

// A service interface
trait PetService[F[_]]:
  def insert(pet: Pet): F[Unit]
  def insert(pets: List[Pet]): F[Unit]
  def selectAll: F[List[Pet]]

// A companion object with constructor and commands/queries
object PetService:
  // Command to insert a pet
  private val insertPet: Command[Pet] =
    sql"INSERT INTO pets VALUES ($varchar, $int4)".command.to[Pet]

  // Command to insert a specific list of pets
  private def insertPets(pets: List[Pet]): Command[pets.type] =
    val petsEncoder = (varchar *: int4).values.to[Pet].list(pets)
    sql"INSERT INTO pets VALUES $petsEncoder".command

  // Query to select all pets
  private val selectAllPets: Query[Void, Pet] =
    sql"SELECT * FROM pets".query(varchar *: int4).to[Pet]

  // PetService constructor
  def fromSession[F[_]: Monad](session: Session[F]): PetService[F] =
    new PetService[F] {
      def insert(pets: List[Pet]): F[Unit] =
        session.prepare(insertPets(pets)).flatMap { pc => pc.execute(pets) }.void

      def insert(pet: Pet): F[Unit] = session.prepare(insertPet).flatMap(_.execute(pet)).void

      def selectAll: F[List[Pet]] = session.execute(selectAllPets)
    }
