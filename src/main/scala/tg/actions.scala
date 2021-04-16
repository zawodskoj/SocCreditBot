package by.oyae.soccredit.bot
package tg

import org.http4s.headers.`Content-Type`
import tg.types.{AnswerInlineQuery, Update, InlineQueryResult, MessageSentResponse, GetUpdatesResponse}
import config.Config

import cats.effect.IO
import org.http4s.multipart.{Multipart, Part, Boundary}
import org.http4s.{EntityEncoder, Uri}
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.MediaType

object actions {
  def baseUrl(implicit config: Config) = s"https://api.telegram.org/bot${config.botToken}"

  def getUpdates(offset: Long)(implicit config: Config, client: Client[IO]): IO[Vector[Update]] =
    client.expect[GetUpdatesResponse](s"$baseUrl/getUpdates?offset=$offset").map(_.result)

  def sendSticker(chatId: Long, bytes: Array[Byte])(implicit config: Config, client: Client[IO]): IO[MessageSentResponse] = {
    val boundary = Boundary.create
    val request = POST(
      Multipart(
        Vector(
          Part.formData[IO]("chat_id", chatId.toString),
          Part.formData[IO]("disable_notification", "true"),
          Part.fileData[IO]("sticker", "sticker.webp", EntityEncoder.byteArrayEncoder[IO].toEntity(bytes).body, `Content-Type`(MediaType.image.webp)),
        ),
        boundary
      ),
      Uri.unsafeFromString(s"$baseUrl/sendSticker"),
      `Content-Type`(MediaType.multipartType("form-data", Some(boundary.value)))
    )

    client.fetchAs[MessageSentResponse](request)
  }

  def deleteMessage(chatId: Long, id: Long)(implicit config: Config, client: Client[IO]): IO[Boolean] = {
    val request = POST(
      s"""{"chat_id":$chatId,"message_id":$id}""",
      Uri.unsafeFromString(s"$baseUrl/deleteMessage"),
      `Content-Type`(MediaType.application.json)
    )

    client.successful(request)
  }

  def answerInlineQuery(queryId: String, sticker: Option[String])(implicit config: Config, client: Client[IO]): IO[Boolean] = {
    val request = POST(
      AnswerInlineQuery(
        queryId,
        sticker.map(code => Vector(
          InlineQueryResult(
            `type` = "sticker", id = "1", sticker_file_id = Some(code)
          )
        )).getOrElse(Vector.empty),
        0
      ),
      Uri.unsafeFromString(s"$baseUrl/answerInlineQuery")
    )

    client.successful(request)
  }
}
