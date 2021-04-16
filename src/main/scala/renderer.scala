package by.oyae.soccredit.bot

import cats.syntax.apply._
import java.nio.file.{Files, Path}
import org.jetbrains.skija._

object renderer {
  private val digits = "一二三四五六七八九"
  private val exponents = "十百千"
  private val zeroMark = '零'
  private val myriadMark = "万"
  private val twoMarkForThousands = '两'

  private val latinSuffixShort = "Soc. Credit"
  private val latinSuffixFull = "Social Credit"

  private val chineseSuffix = "社会信用"

  private val latinTypeface = Typeface.makeFromFile("3rdparty/VCR_OSD_MONO_1.001.ttf")
  private val latinFontLarge = new Font(latinTypeface, 29)
  private val latinFontSmall = new Font(latinTypeface, 24)

  private val cjkTypeface = Typeface.makeFromFile("3rdparty/BIZ-UDGothicR.ttc")
  private val cjkFontLarge = new Font(cjkTypeface, 40)
  private val cjkFontMedium = new Font(cjkTypeface, 36)
  private val cjkFontSmall = new Font(cjkTypeface, 32)
  private val cjkFontPico = new Font(cjkTypeface, 28)

  private val minus = Image.makeFromEncoded(Files.readAllBytes(Path.of("3rdparty/minus.png")))
  private val plus = Image.makeFromEncoded(Files.readAllBytes(Path.of("3rdparty/plus.png")))

  val whitePaint = new Paint()
  whitePaint.setColor(0xffffffff)

  val blackPaint = new Paint()
  blackPaint.setColor(0xff000000)

  def render(base: Image, latinNumber: String, chineseNumber: String): Array[Byte] = {
    val surface = Surface.makeRasterN32Premul(512, 174)
    val canvas = surface.getCanvas

    def renderShadowed(text: String, font: Font, x: Float, y: Float) = {
      val tl = TextLine.make(text, font)

      canvas.drawTextLine(tl, x + 4, y + 4, blackPaint)
      canvas.drawTextLine(tl, x, y, whitePaint)
    }

    canvas.drawImage(base, 0, 0)

    // render chinese number
    val latinYComp = chineseNumber.length match {
      case _4 if _4 <= 4 => renderShadowed(chineseNumber + chineseSuffix, cjkFontLarge, 160, 140); 0
      case 5 => renderShadowed(chineseNumber + chineseSuffix, cjkFontMedium, 160, 140); 0
      case 6 => renderShadowed(chineseNumber + chineseSuffix, cjkFontSmall, 160, 140); 0
      case 7 => renderShadowed(chineseNumber + chineseSuffix, cjkFontPico, 160, 135); 10
      case 8 | 9 | 10 | 11 =>
        renderShadowed(chineseNumber, cjkFontPico, 160, 110)
        renderShadowed(chineseSuffix, cjkFontPico, 160, 145)
        0
      case _ =>
        var splitPosition = (chineseNumber.length + chineseSuffix.length) / 2
        val firstWrappedChar = chineseNumber(splitPosition)
        if (!digits.contains(firstWrappedChar) && firstWrappedChar != zeroMark && firstWrappedChar != twoMarkForThousands)
          splitPosition += 1 // try not to break periods

        val (lp, rp) = chineseNumber.splitAt(splitPosition)
        renderShadowed(lp, cjkFontPico, 160, 110)
        renderShadowed(rp + chineseSuffix, cjkFontPico, 160, 145)
        0
    }

    // render latin number
    val latinSuffix = if (latinNumber.length > 7) latinSuffixShort else latinSuffixFull
    val (latinFont, latinY) = if (latinNumber.length > 4) latinFontSmall -> 75 else latinFontLarge -> 80

    renderShadowed(latinNumber + " " + latinSuffix, latinFont, 160, latinY + latinYComp)

    val image = surface.makeImageSnapshot()
    val data = image.encodeToData(EncodedImageFormat.WEBP)

    data.getBytes
  }

  def renderNumber(origNumber: Int, sig: String, base: Image): Option[Array[Byte]] = {
    val (chineseNumber, latinNumber) = (formatChineseNumber(origNumber), formatLatinNumber(origNumber)).tupled match {
      case Some((l, c)) => (l, c)
      case None => return None
    }

    Some(render(base, sig + latinNumber, sig + chineseNumber))
  }

  def renderRawNumber(amount: Int): Option[Array[Byte]] = {
    if (amount == 0) return None

    if (amount < 0)
      renderNumber(amount, "-", minus)
    else
      renderNumber(amount, "+", plus)
  }

  def formatLatinNumber(number: Int): Option[String] = {
    val abs = number.abs

    if (abs == 0 || abs >= 100000000) return None

    val maxExp = {
      var cur = abs
      var maxExp = 0

      while (cur > 0 && (cur % 10 == 0)) {
        cur /= 10
        maxExp += 1
      }

      maxExp
    }
    maxExp / 3 match {
      case 0 => Some(abs.toString)
      case 1 => Some((abs / 1000) + "k")
      case 2 => Some((abs / 1000000) + "m")
      case _ => None
    }
  }

  def formatChineseNumber(number: Int): Option[String] = {
    var abs = number.abs

    if (abs == 0 || abs >= 100000000 /*一亿*/) return None

    if (abs > 10000 /*一万*/) {
      val lowerPart = {
        val lowerPartInt = abs % 10000
        if (lowerPartInt == 0) "" else formatChineseNumber(lowerPartInt).get
      }

      val upperPart = {
        val upperPartInt = abs / 10000
        formatChineseNumber(upperPartInt).get
      }

      return Some(s"$upperPart$myriadMark$lowerPart")
    }

    var exp = 0
    var result = ""

    while (abs > 0) {
      val digit = abs % 10

      if (digit == 0) {
        if (result.nonEmpty && result(0) != zeroMark)
          result = zeroMark + result
      } else {
        val digitChar = exp match {
          case 3 if digit == 2 => twoMarkForThousands
          case _ => digits(digit - 1)
        }

        val exponent = if (exp == 0) "" else exponents(exp - 1).toString

        result = s"$digitChar$exponent$result"
      }

      abs /= 10
      exp += 1
    }

    Some(result)
  }
}
