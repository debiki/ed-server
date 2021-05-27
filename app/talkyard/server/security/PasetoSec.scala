/**
 * Copyright (c) 2021 Kaj Magnus Lindberg
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package talkyard.server.security

import com.debiki.core._
import com.debiki.core.Prelude._
import debiki.EdHttp._
import dev.paseto.jpaseto.{Paseto => pas_Paseto, Pasetos => pas_Pasetos}
import dev.paseto.jpaseto.{PasetoSecurityException => pas_PasetoSecurityEx}
import dev.paseto.jpaseto.lang.{Keys => pas_Keys}
import javax.crypto.{SecretKey => jc_SecretKey}
import org.scalactic.{Good, Or, Bad}
import org.apache.commons.codec.{binary => acb}



object PasetoSec {


  def decodePasetoV2LocalToken(prefixAndToken: St, symmetricSecret: St): pas_Paseto = {
    val secretKey = decodeSecretKeySt(symmetricSecret)
    val pasetoPrefix = "paseto:"
    throwForbiddenIf(!prefixAndToken.startsWith(pasetoPrefix),
          "TyE603MSJ", "'paseto:...' prefix missing")
    val v2LocalToken: St = prefixAndToken.drop(pasetoPrefix.length)
    val v2LocalDot: St = "v2.local."
    throwForbiddenIf(!v2LocalToken.startsWith(v2LocalDot),
          "TyEUNKTKN4862", o"""PASETO token missing '$v2LocalDot', it instead
          looks like so: "${v2LocalToken.take(25)} …".""")
    val tokenEncrypted: St = v2LocalToken // drop expectedPrefix.length

    // See https://github.com/paseto-toolkit/jpaseto
    // And https://paseto.io/rfc/ for standard claims.
    val token: pas_Paseto = {
      try {
        pas_Pasetos.parserBuilder()
              .setSharedSecret(secretKey)
              .build()
              .parse(tokenEncrypted)
      }
      catch {
        case ex: pas_PasetoSecurityEx =>
          throwBadReq("TyEPASSECEX_", s"Erro/home/user/styd/d9/tests/e2e/utils/utils.tsr parsing Paseto token: ${ex.toString}")
        case ex: Exception =>
          throwBadReq("TyEPARSETKNST", s"Error parsing Paseto token: ${ex.toString}")
      }
    }
    token
  }


  def genPasetoV2LocalSecret(): St = {
    val newKey: jc_SecretKey = pas_Keys.secretKey()
    val newKeyBytes: Array[i8] = newKey.getEncoded
    val keyInHexLower: St = acb.Hex.encodeHexString(newKeyBytes)
    keyInHexLower
  }


  def decodeSecretKeySt(secretSt: St): jc_SecretKey = {
    import org.apache.commons.codec.binary.{Hex => acb_Hex}
    import dev.paseto.jpaseto.lang.{Keys => pas_Keys}
    val secretKey = {
      if (secretSt.isEmpty) {
        throwForbidden("TyE3P3MSEJ4", "PASETO secret key not yet generated and saved")
      }
      else if (secretSt.startsWith("hex:")) {
        val hexSt = secretSt.drop("hex:".length)
        val hexBytes: Array[i8] = acb_Hex.decodeHex(hexSt)
        val key: jc_SecretKey = pas_Keys.secretKey(hexBytes)
        key
      }
      else {
        throwNotImplemented("TYE329067MSTED", "Only secrets in 'hex:...' supported")
      }
    }
    secretKey
  }

}

