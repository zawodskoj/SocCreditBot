package by.oyae.soccredit.bot

import cats.effect.IO
import cats.syntax.either._
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object config {
  case class Config(
    botToken: String,
    toiletUserId: Long,
  )

  def loadConfig: IO[Config] =
    ConfigSource.file(sys.env.getOrElse("STARTUP_CONFIG", "config.hocon"))
      .load[Config].leftMap(v => new Exception(s"Failed to load config: $v")).liftTo[IO]
}
