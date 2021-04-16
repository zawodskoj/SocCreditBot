package by.oyae.soccredit.bot

import renderer.renderRawNumber

import cats.effect.concurrent.Ref
import cats.effect.{IOApp, IO, ExitCode}
import cats.syntax.flatMap._
import cats.syntax.traverse._
import io.circe.syntax._
import org.http4s.Method.POST
import org.http4s.{EntityEncoder, Uri}
import org.http4s.client.Client
import org.http4s.circe._
import org.http4s.multipart.{Multipart, Part, Boundary}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import tg.types._
import config._

import org.http4s.server.blaze.BlazeServerBuilder

object App extends IOApp {
  def loop(config: Config, client: Client[IO]): IO[Unit] = {
    val baseUrl = s"https://api.telegram.org/bot${config.botToken}"

    Ref.of[IO, Long](0).flatMap { ref =>
      implicit val dec = jsonOf[IO, GetUpdatesResponse]

      import org.http4s.client.dsl.io._
      import org.http4s.headers._
      import org.http4s.MediaType

      def reply(q: InlineQuery) = {
        val result = q.query.toIntOption.flatMap { i =>
          renderRawNumber(i)
        }

        def reply(sticker: Option[String]) = {
          val r = POST(
            AnswerInlineQuery(
              q.id,
              sticker.map(code => Vector(
                InlineQueryResult(
                  `type` = "sticker", id = "1", sticker_file_id = Some(code)
                )
              )).getOrElse(Vector.empty),
              0
            ).asJson,
            Uri.unsafeFromString(s"$baseUrl/answerInlineQuery")
          )

          // IO { }
          client.fetchAs[String](r).flatMap(x => IO {
            println(x)
          })
        }

        def sendSticker(bytes: Array[Byte]) = {

          val bry = Boundary.create
          val r2 = POST(
            Multipart(
              Vector(
                Part.formData[IO]("chat_id", config.toiletUserId.toString),
                Part.formData[IO]("disable_notification", "true"),
                Part.fileData[IO]("sticker", "sticker.webp", EntityEncoder.byteArrayEncoder[IO].toEntity(bytes).body, `Content-Type`(MediaType.image.webp)),
              ),
              bry
            ),
            Uri.unsafeFromString(s"$baseUrl/sendSticker"),
            `Content-Type`(MediaType.multipartType("form-data", Some(bry.value)))
          )

          // IO { }
          import io.circe.parser._
          client.fetchAs[String](r2).map(x => decode[MessageSentResponse](x).right.get).flatTap(x => IO {
            println(x)
          })
        }

        def deleteMsg(id: Long) = {
          val r2 = POST(
            s"""{"chat_id":${config.toiletUserId},"message_id":$id}""",
            Uri.unsafeFromString(s"$baseUrl/deleteMessage"),
            `Content-Type`(MediaType.application.json)
          )

          // IO { }
          client.fetchAs[String](r2).flatMap(x => IO { println(x)})
        }

        result match {
          case Some(v) => sendSticker(v) >>= (x => deleteMsg(x.result.message_id) >> reply(Some(x.result.sticker.file_id)))
          case None => reply(None)
        }
      }

      val loopOp = for {
        curOffset <- ref.get
        response <- client.expect[GetUpdatesResponse](s"$baseUrl/getUpdates?offset=$curOffset")
        _ <- ref.set(response.result.maxByOption(_.update_id).map(_.update_id + 1).getOrElse(curOffset))
        _ <- response.result.flatMap(_.inline_query).traverse(reply)
        _ <- IO.sleep(400.millis)
      } yield ()

      loopOp.foreverM
    }
  }

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](global)
      .withHttpApp {
        import org.http4s.dsl.io._
        import org.http4s._
        import org.http4s.headers._
        import org.http4s.syntax.kleisli._

        HttpRoutes.of[IO] {
          case GET -> Root / "render" / IntVar(number) =>
            renderRawNumber(number).map(
              Ok(_, `Content-Type`(MediaType.image.webp))
            ).getOrElse(BadRequest(s"Could not into $number"))
        }.orNotFound
      }
      .bindHttp(9123, "0.0.0.0")
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
      //BlazeClientBuilder[IO](global).resource.use(loop).as(ExitCode.Success)
}
