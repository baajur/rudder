/*
*************************************************************************************
* Copyright 2019 Normation SAS
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

package com.normation.rudder.rest.internal
import com.normation.box._
import com.normation.eventlog._
import com.normation.rudder.repository.EventLogRepository
import com.normation.rudder.rest.RestExtractorService
import com.normation.rudder.rest.RestUtils._
import com.normation.rudder.web.components.DateFormaterService
import com.normation.rudder.web.services._
import net.liftweb.common._
import net.liftweb.http.rest.RestHelper
import net.liftweb.http.{JsonResponse, LiftResponse, Req, S}
import net.liftweb.json.JValue
import net.liftweb.json.JsonDSL._

class EventLogAPI (
    repos: EventLogRepository
  , restExtractor : RestExtractorService
  , eventLogDetail : EventLogDetailsGenerator
) extends  RestHelper with  Loggable {

  def serialize(event: EventLog): JValue = {
    import net.liftweb.json.JsonDSL._

    ( ("id"          -> event.id)
    ~ ("date"        -> DateFormaterService.getFormatedDate(event.creationDate))
    ~ ("actor"       -> event.principal.name)
    ~ ("type"        -> S.?("rudder.log.eventType.names." + event.eventType.serialize))
    ~ ("description" -> eventLogDetail.displayDescription(event).toString)
    ~ ("hasDetails"  -> (if(event.details != <entry></entry>)  true  else  false))
    )
  }

  def errorFormatter(errorMessage : String ) = {
    ( ("draw"            -> 0)
    ~ ("recordsTotal"    -> 0)
    ~ ("recordsFiltered" -> 0)
    ~ ("data"            -> "")
    ~ ("error"           -> errorMessage)
    )
  }
  def responseFormater(draw: Int, totalRecord: Long, totalFiltered: Long, logs: Seq[EventLog]): JValue = {
    ( ("draw"            -> draw)
    ~ ("recordsTotal"    -> totalRecord)
    ~ ("recordsFiltered" -> totalFiltered)
    ~ ("data"            -> logs.map(serialize))
    )
  }

  def getEventLogBySlice(start: Int,  nbelement: Int,  criteria : Option[String], optLimit:Option[Int] = None, orderBy:Option[String]): Box[(Int,Seq[EventLog])] = {
    repos.getEventLogByCriteria(criteria,  optLimit, orderBy).toBox match  {
      case Full(events) => Full((events.size, events.slice(start,start+nbelement)))
      case eb: EmptyBox  => eb ?~! s"Error when trying fetch eventlogs from database for page ${(start/nbelement)+1}"
    }
  }

  def requestDispatch: PartialFunction[Req, () => Box[LiftResponse]] = {
    case Get(Nil, req) =>


      (for {
        optDraw <- restExtractor.extractInt("draw")(req)(i => Full(i.toInt))
        draw <- Box(optDraw) ?~! "Missing 'draw' field from datatable's request"
        optStart <- restExtractor.extractInt("start")(req)(i => Full(i.toInt))
        start <- Box(optStart) ?~! "Missing 'start' field from datatable's request"
        optLength <- restExtractor.extractInt("length")(req)(i => Full(i.toInt))
        length <- Box(optLength) ?~! "Missing 'start' field from datatable's request"
        (totalFiltered, events) <- getEventLogBySlice(start, length, None, None, Some("id DESC"))
        totalRecord <- repos.getEventLogCount.toBox
      } yield {
        responseFormater(draw, totalRecord, totalFiltered.toLong, events)
      }) match {
        case Full(resp) =>
          JsonResponse(resp)
        case eb: EmptyBox =>
          val fail = eb ?~! "Error when fetching event logs"
          JsonResponse(errorFormatter(fail.messageChain), 500)
      }



    case Get(id :: "details" :: Nil, req) =>
      implicit val prettify = restExtractor.extractBoolean("prettify")(req)(identity).getOrElse(Some(false)).getOrElse(false)
      implicit  val action : String = "eventDetails"
      ( for {
        realId <- Box.tryo(id.toLong)
        event  <- repos.getEventLogById(realId).toBox
        crId   = event.id.flatMap(repos.getEventLogWithChangeRequest(_).toBox match {
          case Full(Some((_, crId))) => crId
          case _ => None
        })
        htmlDetails = eventLogDetail.displayDetails(event, crId)
      } yield {
        toJsonResponse(None, "content" -> htmlDetails.toString())
      } ) match {
        case Full(resp) => resp
        case eb : EmptyBox =>
          val fail = eb ?~! s"Error when getting event log with id '${id}' details"
          toJsonError(None, fail.messageChain)
      }
      
  }
  serve("secure" / "api" / "eventlog" prefix requestDispatch)
}
