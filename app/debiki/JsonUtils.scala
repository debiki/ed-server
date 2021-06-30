/**
 * Copyright (c) 2015 Kaj Magnus Lindberg
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package debiki

import com.debiki.core.Prelude._
import com.debiki.core._
import java.{util => ju}
import org.scalactic.{Good, Or, Bad}
import play.api.libs.json._



// Move to  talkyard.server.parse.ParseText?
object ParseText {
  def parseFloat64(text: St, fieldName: St): f64 = {
    try text.toDouble
    catch {
      case _: java.lang.NumberFormatException =>
        // throwBadData instead?
        JsonUtils.throwBadJson("TyE06MPGN2", s"Field $fieldName is not a number")
      case _: java.lang.NullPointerException =>
        JsonUtils.throwBadJson("TyE06MPGN3", s"Field $fieldName is null")
    }
  }
}



/** Parses JSON. Throws human friendly IllegalArgumentException:s. Is,a bit more concise
  * than Play's built in stuff.
  */
object JsonUtils {   MOVE // to talkyard.server.parser.JsonParSer

  RENAME // to BadDataEx? And change TyJson, TyMap, TyPaseto so they get
  // an extra param: inclStackTraceInErrors: Bo = false.
  // If no stack trace, throw BadInpDataEx.
  // Otherwise, throw BadDataEx *with* a stack trace.
  class BadJsonException(message: String) extends IllegalArgumentException(message)


  // This is year 5138 in Unix time in seconds, but year 1973 in milliseconds.
  // If we see any unix time greater than this, we'll assume it's in millis, otherwise, seconds.
  private val UnixMillisSomeDayIn1973 = 100000000000L  // [MINMILLIS]

  def tryParseGoodBad[R](block: => R Or ErrMsg): R Or ErrMsg = {
    try block
    catch {
      case ex: BadJsonException =>
        Bad(ex.getMessage)
    }
  }

  def tryParse[R](block: => R): R Or ErrMsg = {
    try Good(block)
    catch {
      case ex: BadJsonException =>
        Bad(ex.getMessage)
    }
  }

  def asJsObject(json: JsValue, what: St): JsObject =
    json match {
      case o: JsObject => o
      case _ => throwBadJson("TyE0JSOBJ", s"$what is not a JsObject")
    }

  def asJsArray(json: JsValue, what: St): Seq[JsValue] =
    json match {
      case a: JsArray => a.value
      case _ => throwBadJson("TyE0JSARR", s"$what is not a JsArray")
    }

  def parseJsObject(json: JsValue, fieldName: St): JsObject =
    readJsObject(json, fieldName)

  def readJsObject(json: JsValue, fieldName: St): JsObject =
    readOptJsObject(json, fieldName).getOrElse(throwMissing("EsE1FY90", fieldName))

  def parseOptJsObject(json: JsValue, fieldName: St): Opt[JsObject] =
    readOptJsObject(json, fieldName)

  def readOptJsObject(json: JsValue, fieldName: St): Opt[JsObject] =
    (json \ fieldName).toOption map {
      case o: JsObject => o
      case JsNull => return None
      case bad =>
        throwBadJson(
          "EsE2YMP7", s"'$fieldName' is not a JsObject, but a ${classNameOf(bad)}")
    }

  def parseJsArray(json: JsValue, fieldName: St, optional: Bo = false): Seq[JsValue] =
    readJsArray(json, fieldName, optional).value

  // Add a 2nd fn, or a param: all elems be of the same type? See below: [PARSEJSARR]
  def readJsArray(json: JsValue, fieldName: St, optional: Bo = false): JsArray = {
    val array = (json \ fieldName).toOption getOrElse {
      if (optional) return JsArray()
      throwMissing("TyE0JSFIELD", fieldName)
    }
    array match {
      case o: JsArray => o
      case bad =>
        throwBadJson(
          "EsE4GLK3", s"'$fieldName' is not a JsArray, but a ${classNameOf(bad)}")
    }
  }

  /*
  // No way to shorten this?  [PARSEJSARR]
  (jsObj \ "newTopicTypes").asOpt[Seq[JsValue]] match {
    case Some(list) if list.isInstanceOf[JsArray] =>
      // Should be only one topic type. [5YKW294]
      list.asInstanceOf[JsArray].value.headOption match {
        case Some(jsValue) =>
          jsValue match {
            case JsNumber(number) => PageType.fromInt(number.toInt).toVector
            case _ => Nil
          }
        case _ => Nil
      }
    case _ => Nil
  } */

  def parseStOrNrAsSt(json: JsValue, fieldName: St, altName: St = ""): St =
    parseStOrNrAsOptSt(json, fieldName, altName) getOrElse throwMissing(
          "TyE60RMP25R", fieldName)

  def parseStOrNrAsOptSt(json: JsValue, fieldName: St, altName: St = ""): Opt[St] =
    (json \ fieldName).asOpt[JsValue].orElse((json \ altName).asOpt[JsValue]) map {
      case n: JsNumber => n.value.toString
      case s: JsString => s.value
      case bad =>
        throwBadJson("TyE503MRG",
            s"'$fieldName' is not a string or number, it is a: ${classNameOf(bad)}")
    }



  def parseSt(json: JsValue, fieldName: St, altName: St = ""): St =
    parseOptSt(json, fieldName, altName) getOrElse throwMissing(
          "TyE5S204RTE", fieldName)

  def readString(json: JsValue, fieldName: St, maxLen: i32 = -1): St =
    readOptString(json, fieldName, maxLen = maxLen) getOrElse throwMissing(
          "EsE7JTB3", fieldName)


  /** If noneIfLongerThan is >= 0, returns None if the value is longer than that.
    */
  def parseOptSt(json: JsValue, fieldName: St, altName: St = "",
        noneIfLongerThan: i32 = -1, cutAt: i32 = -1): Opt[St] = {
    var anySt = readOptString(json, fieldName, altName)
    dieIf(noneIfLongerThan >= 0 && cutAt >= 0,
          "TyE7PM506RP", "Both noneIfLongerThan and cutAt specified")
    if (cutAt >= 0) {
      anySt = anySt.map(_.take(cutAt))
    }
    if (noneIfLongerThan >= 0) {
      anySt foreach { value =>
        if (value.length > noneIfLongerThan)
          return None
      }
    }
    anySt
  }

  def readOptString(json: JsValue, fieldName: St, altName: St = "",
          maxLen: i32 = -1): Opt[St] = {
    val primaryResult = readOptStringImpl(json, fieldName, maxLen = maxLen)
    if (primaryResult.isDefined || altName.isEmpty) primaryResult
    else readOptStringImpl(json, altName, maxLen = maxLen)
  }

  private def readOptStringImpl(json: JsValue, fieldName: St, maxLen: i32 = -1)
          : Opt[St] =
    (json \ fieldName).validateOpt[String] match {
      case JsSuccess(value, _) =>
        value map { textUntrimmed =>
          val text = textUntrimmed.trim
          throwBadJsonIf(0 <= maxLen && maxLen < text.length,
                "TyE7MW3RMJ5", s"'$fieldName' is too long: ${text.length
                } chars, only $maxLen allowed")
          text
        }
      case JsError(errors) =>
        // Will this be readable? Perhaps use json.value[fieldName] match ... instead, above.
        throwBadJson("EsE5GUMK", s"'$fieldName' is not a string: " + errors.toString())
    }


  def readParsedRef(json: JsValue, fieldName: String, allowParticipantRef: Boolean): ParsedRef = {
    val refStr = readString(json, fieldName)
    com.debiki.core.parseRef(refStr, allowParticipantRef = allowParticipantRef) getOrIfBad { problem =>
      throwBadJson("TyEBADREFFLD", s"Field '$fieldName': Bad ref: '$refStr', the problem: $problem")
    }
  }


  def readOptByte(json: JsValue, fieldName: String): Option[Byte] = {
    readOptLong(json, fieldName) map { valueAsLong =>
      if (valueAsLong > Byte.MaxValue)
        throwBadJson("EsE4GK2W0", s"$fieldName is too large for a Byte: $valueAsLong")
      if (valueAsLong < Byte.MinValue)
        throwBadJson("EsE4GKUP02", s"$fieldName is too small for a Byte: $valueAsLong")
      valueAsLong.toByte
    }
  }


  def parseF32(json: JsValue, field: St, alt: St = "", default: Opt[f32] = None): f32 =
    readFloat(json, field, alt, default)


  def readFloat(json: JsValue, fieldName: String, altName: String = "", default: Option[Float] = None): Float =
    readOptFloat(json, fieldName, altName = altName).orElse(default)
      .getOrElse(throwMissing("TyE06KA2P2", fieldName))


  def parseOptFloat32(json: JsValue, fieldName: String, altName: String = ""): Opt[f32] =
    readOptFloat(json, fieldName, altName = altName)

  def readOptFloat(json: JsValue, fieldName: String, altName: String = ""): Option[Float] = {
    readOptDouble(json, fieldName).orElse(readOptDouble(json, altName)) map { valAsDouble =>
      if (valAsDouble > Float.MaxValue)
        throwBadJson("TyE603WMDC7", s"$fieldName is too large for a Float: $valAsDouble")
      if (valAsDouble < Float.MinValue)
        throwBadJson("TyE20XKD38", s"$fieldName is too small for a Float: $valAsDouble")
      valAsDouble.toFloat
    }
  }


  def readDouble(json: JsValue, fieldName: String, altName: String = "",
        default: Option[Double] = None): Double =
    parseFloat64(json, fieldName = fieldName, altName = altName, default = default)


  def parseFloat64(json: JsValue, fieldName: St, altName: St = "",
        default: Opt[f64] = None): f64 =
    readOptDouble(json, fieldName).orElse(readOptDouble(json, altName)).orElse(default)
      .getOrElse(throwMissing("TyE078RVF3", fieldName))


  def readOptDouble(json: JsValue, fieldName: String): Option[Double] = {
    (json \ fieldName).validateOpt[Double] match {
      case JsSuccess(value, _) => value
      case JsError(errors) =>
        throwBadJson("TyE603RMDJV", s"'$fieldName' is not a Double: " + errors.toString())
    }
  }


  def parseInt32(json: JsValue, field: St, alt: St = "", default: Opt[i32] = None): i32 =
    readInt(json, fieldName = field, altName = alt, default = default)


  def parseI32(json: JsValue, field: St, alt: St = "", default: Opt[i32] = None): i32 =
    readInt(json, field, alt, default)


  def readInt(json: JsValue, fieldName: String, altName: String = "",
        default: Option[Int] = None): Int =
    readOptInt(json, fieldName).orElse(readOptInt(json, altName)).orElse(default)
      .getOrElse(throwMissing("EsE5KPU3", fieldName))


  def parseOptI32(json: JsValue, field: St, altField: St = ""): Opt[i32] =
     readOptInt(json, field, altField)


  def parseOptInt32(json: JsValue, field: St, altField: St = ""): Opt[i32] =
     readOptInt(json, fieldName = field, altName = altField)


  def readOptInt(json: JsValue, fieldName: String, altName: String = ""): Option[Int] = {
    readOptLong(json, fieldName).orElse(readOptLong(json, altName)) map { valueAsLong =>
      if (valueAsLong > Int.MaxValue)
        throwBadJson("EsE5YKP02", s"$fieldName is too large for an Int: $valueAsLong")
      if (valueAsLong < Int.MinValue)
        throwBadJson("EsE2PK6S3", s"$fieldName is too small for an Int: $valueAsLong")
      valueAsLong.toInt
    }
  }


  def readLong(json: JsValue, fieldName: String): Long =
    readOptLong(json, fieldName) getOrElse throwMissing("EsE6Y8FW2", fieldName)

  def parseOptLong(json: JsValue, fieldName: St): Opt[i64] =
    readOptLong(json, fieldName)

  def parseOptInt64(json: JsValue, fieldName: St): Opt[i64] =
    readOptLong(json, fieldName = fieldName)

  def readOptLong(json: JsValue, fieldName: String): Option[Long] =
    (json \ fieldName).validateOpt[Long] match {
      case JsSuccess(value, _) => value
      case JsError(errors) =>
        // Will this be readable? Perhaps use json.value[fieldName] match ... instead, above.
        throwBadJson("EsE3GUK7", s"'$fieldName' is not an integer: " + errors.toString())
    }


  def parseBo(json: JsValue, fieldName: St, default: Opt[Bo] = None): Bo =
    readOptBool(json, fieldName).orElse(default) getOrElse throwMissing(
          "TyE603MFE67", fieldName)

  def parseBoDef(json: JsValue, fieldName: St, default: Bo): Bo =
    readOptBool(json, fieldName) getOrElse default

  def readBoolean(json: JsValue, fieldName: String): Boolean =
    readOptBool(json, fieldName) getOrElse throwMissing("EsE4GUY8", fieldName)


  def parseOptBo(json: JsValue, fieldName: St): Opt[Bo] =
    readOptBool(json, fieldName)

  def readOptBool(json: JsValue, fieldName: String): Option[Boolean] =
    (json \ fieldName).validateOpt[Boolean] match {
      case JsSuccess(value, _) => value
      case JsError(errors) =>
        // Will this be readable? Perhaps use json.value[fieldName] match ... instead, above.
        throwBadJson("EsE2GKU8", s"'$fieldName' is not a boolean: " + errors.toString())
    }


  def readWhen(json: JsValue, fieldName: String): When =
    When.fromDate(readDateMs(json, fieldName: String))


  def readWhenDay(json: JsValue, fieldName: String): WhenDay =
    WhenDay.fromDate(readDateMs(json, fieldName: String))


  def readOptWhen(json: JsValue, fieldName: String): Option[When] =
    readOptDateMs(json, fieldName).map(When.fromDate)


  def readDateMs(json: JsValue, fieldName: String): ju.Date =
    readOptDateMs(json, fieldName) getOrElse throwMissing("EsE2PKU0", fieldName)


  def readOptDateMs(json: JsValue, fieldName: String): Option[ju.Date] = {
    // Backw compat: support both ...AtMs  and  ...At
    val jsValue: JsLookupResult = {
      (json \ fieldName).orElse({
        if (fieldName.endsWith("AtMs")) json \ fieldName.dropRight(2)
        else JsUndefined(s"Field missing: $fieldName")
      })
    }
    jsValue.validateOpt[Long] match {
      case JsSuccess(value, _) =>
        val dateMs = value getOrElse {
          return None
        }
        if (dateMs < UnixMillisSomeDayIn1973 && dateMs != 0) {
          throwBadJson("EsE7UMKW2", o"""'$fieldName' looks like a unix time in seconds,
              should be milliseconds""")
        }
        Some(new ju.Date(dateMs))
      case JsError(errors) =>
        // Will this be readable? Perhaps use json.value[fieldName] match ... instead, above.
        throwBadJson("EsE5YYW2", s"'$fieldName' is not a number: " + errors.toString())
    }
  }

  private def throwBadJsonIf(test: => Bo, errCode: St, message: St): U =
    if (test) throwBadJson(errCode, message)

  def throwBadJson(errorCode: String, message: String) =
    throw new BadJsonException(s"$message [$errorCode]")


  private def throwMissing(errorCode: String, fieldName: String) =
    throwBadJson(errorCode, s"'$fieldName' field missing")

}
