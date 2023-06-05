package queries.service

import skunk.Session
import skunk.Query
import skunk.Void
import skunk.syntax.all.sql
import skunk.codec.all.text
import skunk.codec.all.int4
import skunk.codec.all.bpchar
import skunk.codec.all.varchar
import skunk.codec.all.timestamptz
import fs2.Stream
import cats.Applicative
import cats.syntax.all.toFunctorOps
import java.time.OffsetDateTime

// A service interface
trait Service[F[_]]:
  def currentTimestamp: F[OffsetDateTime]
  def countriesByName(pattern: String): Stream[F, Country]

// A companion with construnctor for service
object Service:
  private val timestamp: Query[Void, OffsetDateTime] =
    sql"SELECT CURRENT_TIMESTAMP".query(timestamptz)

  private val countries: Query[String, Country] =
    sql"""
      SELECT name, code, population
      FROM country
      WHERE name LIKE $text
    """
      .query(varchar *: bpchar(3) *: int4)
      .to[Country]

  def fromSession[F[_]: Applicative](session: Session[F]): F[Service[F]] =
    session.prepare(countries).map { pq =>
      // Our service implementation.
      // Note that we are preparing the query on construction, so
      // our service can run it many times without paying the planning cost again.
      new Service[F] {
        def currentTimestamp: F[OffsetDateTime] = session.unique(timestamp)

        def countriesByName(pattern: String): Stream[F, Country] =
          pq.stream(pattern, 32)
      }
    }
