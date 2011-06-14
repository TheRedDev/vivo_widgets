package edu.duke.oit.vw.actor

import scala.actors.Actor
import scala.actors.Actor._

import com.hp.hpl.jena.rdf.model.{Model => JModel, ModelFactory}
import edu.duke.oit.vw.connection._
import com.hp.hpl.jena.tdb.TDBFactory
import edu.duke.oit.vw.sparql.Sparqler

case object Graph

case class SetModel(model: JModel)

object JenaActor extends Actor {

  def act = {
    var model: Option[JModel] = None
    loop {
      react {
        case SetModel(m: JModel) => model = Some(m)
        case Graph => {
          reply(model)
        }
        case _ => println("JenaActor message not found.")
      }
    }
  }

}

object JenaCache {

  val jenaActor = JenaActor.start

  def setFromDatabase(cInfo: JenaConnectionInfo, modelUri: String) {
    Jena.sdbModel(cInfo, modelUri) {
      dbModel =>
        var model = TDBFactory.createModel // ModelFactory.createDefaultModel
        model.add(dbModel)
        Jena.sdbModel(cInfo,"http://vitro.mannlib.cornell.edu/filegraph/tbox/vivo-core-1.2.owl") { vivoOwl => model.add(vivoOwl) }
        setModel(model)
    }

  }

  def setModel(m: JModel) = {
    jenaActor ! new SetModel(m)
  }

  def getModel: Option[JModel] = {
    (jenaActor !? Graph) match {
      case Some(m: JModel) => Some(m)
      case _ => None
    }
  }

  def queryModel(query: String) = {
    val model = jenaActor !? Graph
    model match {
      case Some(m: JModel) => {
        Sparqler.selectingFromModel(m,query){resultsSet => Sparqler.simpleResults(resultsSet)}
      }
      case _ => List()
    }
  }

}
