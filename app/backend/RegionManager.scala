package backend

import akka.actor.Actor
import akka.actor.Props
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import akka.routing.FromConfig

object RegionManager {
  case class UpdateUserPosition(regionId: RegionId, userPosition: UserPosition) extends ConsistentHashable {
    override def consistentHashKey: Any = regionId
  }

  case class UpdateRegionPoints(regionId: RegionId, regionPoints: RegionPoints) extends ConsistentHashable {
    override def consistentHashKey: Any = regionId
  }

}

class RegionManager extends Actor {
  import RegionManager._

  val regionManagerRouter = context.actorOf(Props.empty.withRouter(FromConfig), "router")

  def receive = {
    case UpdateUserPosition(regionId, userPosition) =>
      context.child(regionId.name) match {
        case Some(ref) => ref ! userPosition
        case None      => context.actorOf(Props(new Region(regionId)), regionId.name) ! userPosition
      }

    case UpdateRegionPoints(regionId, regionPoints) =>
      context.child(regionId.name) match {
        case Some(ref) => ref ! regionPoints
        case None      => context.actorOf(Props(new SummaryRegion(regionId)), regionId.name) ! regionPoints
      }

    case p @ RegionPoints(regionId, _) =>


      // count reported by child region, propagate it to summary region on responsible node
      regionId.summaryRegionId foreach { summaryRegionId =>
        regionManagerRouter ! UpdateRegionPoints(summaryRegionId, p)
      }
  }

}