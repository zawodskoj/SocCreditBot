package by.oyae.soccredit.bot

import renderer.renderRawNumber

import cats.effect.concurrent.Ref
import cats.effect.{IOApp, IO, ExitCode}
import cats.syntax.flatMap._
import cats.syntax.apply._
import cats.syntax.traverse._
import org.http4s.client.Client

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import tg.types._
import tg.actions._
import config._

import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

object App extends IOApp {
  def loop(config: Config)(client: Client[IO]): IO[Unit] = {
    implicit val _cf: Config = config
    implicit val _cl: Client[IO] = client

    (Ref.of[IO, Long](0), Ref.of[IO, Long](0)).tupled.flatMap { case (curOffsetRef, toiletMessageRef) =>
      def reply(q: InlineQuery) = {
        val result = q.query.toIntOption.flatMap { i =>
          renderRawNumber(i)
        }

        result match {
          case Some(v) => for {
            stickerMsg <- sendSticker(config.toiletUserId, v)
            _ <- deleteMessage(config.toiletUserId, stickerMsg.result.message_id)
            _ <- answerInlineQuery(q.id, Some(stickerMsg.result.sticker.file_id))
          } yield ()
          case None => answerInlineQuery(q.id, None)
        }
      }

      val loopOp = for {
        updates <- curOffsetRef.get >>= getUpdates
        _ <- updates.maxByOption(_.update_id).map(_.update_id + 1).traverse(curOffsetRef.set)
        _ <- updates.flatMap(_.inline_query).traverse(reply)
        _ <- IO.sleep(400.millis)
      } yield ()

      loopOp.foreverM
    }
  }

  val testServer: IO[ExitCode] =
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

  override def run(args: List[String]): IO[ExitCode] =
    loadConfig >>= (c => BlazeClientBuilder[IO](global).resource.use(loop(c)).as(ExitCode.Success))
}
