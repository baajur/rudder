/*
*************************************************************************************
* Copyright 2011 Normation SAS
*************************************************************************************
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
*
* In accordance with the terms of section 7 (7. Additional Terms.) of
* the GNU Affero GPL v3, the copyright holders add the following
* Additional permissions:
* Notwithstanding to the terms of section 5 (5. Conveying Modified Source
* Versions) and 6 (6. Conveying Non-Source Forms.) of the GNU Affero GPL v3
* licence, when you create a Related Module, this Related Module is
* not considered as a part of the work and may be distributed under the
* license agreement of your choice.
* A "Related Module" means a set of sources files including their
* documentation that, without modification of the Source Code, enables
* supplementary functions or services in addition to those offered by
* the Software.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/agpl.html>.
*
*************************************************************************************
*/

package com.normation.rudder.domain.nodes

import com.normation.inventory.domain.AgentType
import org.joda.time.DateTime
import com.normation.inventory.domain.NodeId
import com.normation.utils.HashcodeCaching
import com.normation.inventory.domain.Agent

/**
 * A NodeInfo is a read only object containing the information that will be
 * always useful about a node
 * @author Nicolas CHARLES
 *
 */
case class NodeInfo(
    id            : NodeId
  , name          : String
  , description   : String
  , hostname      : String
  , os            : String
//  , ips           : List[String]
  , inventoryDate : DateTime
  , agents        : Seq[Agent]
  , localAdministratorAccountName: String
  , creationDate  : DateTime
  , isBroken      : Boolean
  , isSystem      : Boolean
) extends HashcodeCaching 

/**
 * A PolicyServerNodeInfo is basically the same as a NodeInfo, except that
 * it refers to a PolicyServer, which has no inventory, and must be self sufficient
 * in the Node branch  
 * @author Nicolas CHARLES
 *
 */
case class PolicyServerNodeInfo(
    override val id            : NodeId
  , override val name          : String
  , override val description   : String
  , override val hostname      : String
 // , override val ips           : List[String]
  , override val inventoryDate : DateTime
  , override val agents        : Seq[Agent]
  , override val localAdministratorAccountName : String
  , override val creationDate  : DateTime
  , override val isBroken      : Boolean
  , override val isSystem      : Boolean
) extends NodeInfo(id, name, description,hostname,os = "",/* ips,*/ inventoryDate, agents, localAdministratorAccountName, creationDate, isBroken, isSystem) with HashcodeCaching 
