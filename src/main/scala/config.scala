package by.oyae.soccredit.bot

import pureconfig.generic.auto._

object config {
  case class Config(
    botToken: String,
    toiletUserId: Long,
  )
}
