package talkyard.server.parser

import com.debiki.core._
import com.debiki.core.Prelude.classNameOf
import java.util.{Map => j_Map}



object TyMap {

  // Or move to opaque type SignOnId?  [Scala_3]
  def parseSignOnId(idMaybe: St): SignOnId = {
    if (idMaybe.isEmpty) throwBadInpData("TyE5603MRE245", "Sign-On ID is empty")
    idMaybe
  }


  def parseSt(map: j_Map[St, AnyRef], fieldName: St): St = {
    parseOptSt(map, fieldName) getOrElse {
      throwBadInpData("TyE46MR496", s"Field missing: '$fieldName', should be text")
    }
  }


  def parseOptSt(map: j_Map[St, AnyRef], fieldName: St): Opt[St] = {
    val value: AnyRef = map.get(fieldName)
    if (value eq null) return None
    if (!value.isInstanceOf[String]) None // throw?
    else Some(value.asInstanceOf[String])
  }


  def parseOptBo(map: j_Map[St, AnyRef], fieldName: St): Opt[Bo] = {
    val value: AnyRef = map.get(fieldName)
    if (value eq null) return None
    if (!value.isInstanceOf[Bo]) None // or throw?
    else Some(value.asInstanceOf[Bo])
  }


  def parseInt64(map: j_Map[St, AnyRef], fieldName: St): i64 = {
    parseOptInt64(map, fieldName) getOrElse {
      throwBadInpData("TyE60RSM2", s"Field missing: '$fieldName', should be an integer")
    }
  }


  def parseOptInt64(map: j_Map[St, AnyRef], fieldName: St): Opt[i64] = {
    val value: AnyRef = map.get(fieldName)
    if (value eq null) return None
    if (value.isInstanceOf[i32]) Some(value.asInstanceOf[i32].toLong)
    else if (value.isInstanceOf[i64]) Some(value.asInstanceOf[i64])
    else None
  }


  def parseNestedMap(map: j_Map[St, AnyRef], fieldName: St): j_Map[St, AnyRef] = {
    parseOptNestedMap(map, fieldName) getOrElse {
      throwBadInpData("TyE46MREJ2",
            s"Field missing: '$fieldName', should be a nested map")
    }
  }


  def parseOptNestedMap(map: j_Map[St, AnyRef], fieldName: St): Opt[j_Map[St, AnyRef]] = {
    val value: AnyRef = map.get(fieldName)
    value match {
      case null => None
      case m: j_Map[St, AnyRef] => Some(m)
      case _ =>
        throwBadInpData("TyE603MATE24",
              s"'$fieldName' is not a nested map, but a ${classNameOf(value)}")
    }
  }

}
