package by.oyae.soccredit.bot

import java.nio.file.{Files, Path}
import org.jetbrains.skija._

object renderer {
  private val chineseDigits = "一二三四五六七八九"

  private val latinTypeface = Typeface.makeFromFile("3rdparty/VCR_OSD_MONO_1.001.ttf")
  private val latinFont = new Font(latinTypeface, 29)

  private val cjkTypeface = Typeface.makeFromFile("3rdparty/BIZ-UDGothicR.ttc")
  private val cjkFont = new Font(cjkTypeface, 40)

  private val minus = Image.makeFromEncoded(Files.readAllBytes(Path.of("3rdparty/minus.png")))
  private val plus = Image.makeFromEncoded(Files.readAllBytes(Path.of("3rdparty/plus.png")))

  val whitePaint = new Paint()
  whitePaint.setColor(0xffffffff)

  val blackPaint = new Paint()
  blackPaint.setColor(0xff000000)

  def render(base: Image, latinText: String, chineseText: String): Array[Byte] = {
    val surface = Surface.makeRasterN32Premul(512, 174)
    val canvas = surface.getCanvas

    def renderShadowed(text: String, font: Font, x: Float, y: Float) = {
      val tl = TextLine.make(text, font)

      canvas.drawTextLine(tl, x + 4, y + 4, blackPaint)
      canvas.drawTextLine(tl, x, y, whitePaint)
    }

    canvas.drawImage(base, 0, 0)
    renderShadowed(latinText, latinFont, 160, 80)
    renderShadowed(chineseText, cjkFont, 160, 140)

    val image = surface.makeImageSnapshot()
    val data = image.encodeToData(EncodedImageFormat.WEBP)

    data.getBytes
  }

  def renderNumber(sig: String, digit: Int, power: Int, base: Image): Option[Array[Byte]] = {
    val (latinNumber, chineseNumber) = power match {
      case 0 => digit.toString -> chineseDigits.charAt(digit - 1)
      case 1 => (digit.toString + "0") -> (chineseDigits.charAt(digit - 1) + "十")
      case 2 => (digit.toString + "00") -> (chineseDigits.charAt(digit - 1) + "百")
      case 3 => (digit.toString + "k") -> (chineseDigits.charAt(digit - 1) + "千")
      case 4 => (digit.toString + "0k") -> (chineseDigits.charAt(digit - 1) + "万")
      case _ => return None
    }

    Some(render(base, s"$sig$latinNumber Social Credit", s"$sig${chineseNumber}社会信用"))
  }

  def renderRawNumber(amount: Int): Option[Array[Byte]] = {
    if (amount == 0) return None
    val mod = if (amount < 0) -amount else amount

    val log = Math.floor(Math.log10(mod)).toInt
    val digit = mod / Math.pow(10, log).toInt

    if (amount < 0)
      renderNumber("-", digit, log, minus)
    else
      renderNumber("+", digit, log, plus)
  }
}
