package by.oyae.soccredit.bot
package tg

import io.circe.generic.JsonCodec

object types {
  @JsonCodec case class InlineQuery(id: String, query: String, offset: String)
  @JsonCodec case class InputMessageContent(message_text: String)
  @JsonCodec case class InlineQueryResult(`type`: String, id: String, sticker_file_id: Option[String] = None)
  @JsonCodec case class AnswerInlineQuery(inline_query_id: String, results: Vector[InlineQueryResult], cache_time: Long)
  @JsonCodec case class UploadStickerFile(user_id: Long, png_sticker: String)
  @JsonCodec case class Message(message_id: Long, sticker: Sticker)
  @JsonCodec case class Sticker(file_id: String)
  @JsonCodec case class Update(update_id: Long, inline_query: Option[InlineQuery])
  @JsonCodec case class GetUpdatesResponse(result: Vector[Update])
  @JsonCodec case class MessageSentResponse(result: Message)
}
