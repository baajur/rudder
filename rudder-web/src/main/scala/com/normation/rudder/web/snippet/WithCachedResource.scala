/*
*************************************************************************************
* Copyright 2017 Normation SAS
*************************************************************************************
*
* This file is part of Rudder.
*
* Rudder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* In accordance with the terms of section 7 (7. Additional Terms.) of
* the GNU General Public License version 3, the copyright holders add
* the following Additional permissions:
* Notwithstanding to the terms of section 5 (5. Conveying Modified Source
* Versions) and 6 (6. Conveying Non-Source Forms.) of the GNU General
* Public License version 3, when you create a Related Module, this
* Related Module is not considered as a part of the work and may be
* distributed under the license agreement of your choice.
* A "Related Module" means a set of sources files including their
* documentation that, without modification of the Source Code, enables
* supplementary functions or services in addition to those offered by
* the Software.
*
* Rudder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Rudder.  If not, see <http://www.gnu.org/licenses/>.

*
*************************************************************************************
*/

package com.normation.rudder.web.snippet

import net.liftweb.common.Box
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.http.DispatchSnippet
import net.liftweb.http.LiftRules
import scala.xml.Elem
import scala.xml.MetaData
import scala.xml.NodeSeq
import scala.xml.Null
import scala.xml.UnprefixedAttribute


/**
 *
 * This class transform script and img "src" and link "href" so
 * that the path is appended with an unique identifier.
 * These path will be processed by a stateless dispatch in Boot.scala.
 *
 * The code is almost a copy of net.liftweb.builtin.snippet.WithResourceId
 * but with img managed.
 */
object WithCachedResource extends DispatchSnippet {
  def dispatch: DispatchIt = {
    case _ =>  render
  }

  def render(xhtml: NodeSeq): NodeSeq = {
    xhtml flatMap (_ match {
     case e: Elem if e.label == "link" =>
        attrStr(e.attributes, "href").map { href =>
          e.copy(attributes =
            MetaData.update(e.attributes,
                            e.scope,
                            new UnprefixedAttribute("href", LiftRules.attachResourceId(href), Null))
          )
        } openOr e
     case e: Elem if(e.label == "script" || e.label == "img") =>
        attrStr(e.attributes, "src") map { src =>
          e.copy(attributes =
             MetaData.update(e.attributes,
                             e.scope,
                             new UnprefixedAttribute("src", LiftRules.attachResourceId(src), Null))
          )
        } openOr e
     case e => e
    })
  }


  private def attrStr(attrs: MetaData, attr: String): Box[String] = (attrs.get(attr) match {
    case None => Empty
    case Some(Nil) => Empty
    case Some(x) => Full(x.toString)
  }) or (attrs.get(attr.toLowerCase) match {
    case None => Empty
    case Some(Nil) => Empty
    case Some(x) => Full(x.toString)
  })
}
