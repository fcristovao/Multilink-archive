package multilink.client

import akka.actor._
import multilink.network.Gateway
import akka.util.duration._

object Dialer {
	sealed trait States
	case object Idle extends States
	case object Dialing extends States
	case object AwaitingConfirmation extends States
	case object Connected extends States
	

	sealed trait Messages
	case class Dial(route: List[Int]) extends Messages
	case object Disconnect extends Messages
	private case object Timeout extends Messages
	
	type Data = (Map[Int, Either[(Int, Int), Int]], Set[ActorRef], Option[Int])
	val NullData: Data = (Map.empty, Set.empty, None)
}

class Dialer(internetPointsDatabase: ActorRef) extends Actor with FSM[Dialer.States, Dialer.Data] with LoggingFSM[Dialer.States, Dialer.Data]{
	import Dialer._
	import InternetPointsDatabase._
	import FSM._
	
	def getActorRefs(routingList:List[Int]): Map[Int, Either[(Int, Int), Int]] = {
		def helper(routingList:List[Int], state: Map[Int, Either[(Int, Int), Int]]) : Map[Int, Either[(Int, Int), Int]] = {
			routingList match {
				case from :: (rest @  through :: to :: _ ) => {
					internetPointsDatabase ! Get(through); 
					helper(rest, state + (through -> Left((from, to))))
				}
				case from :: to :: Nil => {
					internetPointsDatabase ! Get(to); 
					 state + (to -> Right(from))
				}
				case _ :: Nil => state
				case Nil => state
			}
		}
		
		helper(routingList, Map.empty)
	}
	
	startWith(Idle, NullData)
	
	when(Idle){
		case Ev(Dial(routingList)) => 
			goto(Dialing) using ((getActorRefs(routingList), Set.empty, Some(routingList.last)))
		case Ev(_) => stay
	}
	
	when(Dialing){
		case Event(IPFor(id, None), _) => goto(Idle) using NullData //dialing failed, one id couldn't be found 
		case Event(IPFor(id, Some(actor)), (mapOfIDs, setOfSentMsgs, finalDestination)) =>{
			mapOfIDs(id) match {
				case Left((from, to)) => actor ! Gateway.Route(from, id ,to) 
				case Right(from) => actor ! Gateway.Connect(from, id)
			}
			
			val tmp = mapOfIDs - id;
			
			if(tmp.isEmpty)
				goto(AwaitingConfirmation) using (tmp, setOfSentMsgs + actor, finalDestination)
			else 
				stay using (tmp, setOfSentMsgs + actor, finalDestination)
		}
		case Event(Gateway.Routed(_,_,_), (map, setOfSentMsgs, finalDestination)) => {
			stay using (map, setOfSentMsgs - self.sender.get, finalDestination)
		}
		
		case Event(Gateway.Connected(from, to), (map, setOfSentMsgs, finalDestination)) => {
			stay using (map, setOfSentMsgs - self.sender.get, finalDestination)
		}
		case Event(Timeout, _) => goto(Idle) using NullData 
	}
	
	when(AwaitingConfirmation){
		case Event(Gateway.Routed(_,_,_), (map, setOfSentMsgs, finalDestination)) => {
			val tmp = setOfSentMsgs - self.sender.get // this doesn't work... We have to use IDs
			if(tmp.isEmpty) {
				//Everything worked
				goto(Connected) 
			} else {
				//We still have answers to await for
				stay
			} using (map, tmp, finalDestination)
		}
		
		case Event(Gateway.Connected(from, to), (map, setOfSentMsgs, finalDestination)) => {
			val tmp = setOfSentMsgs - self.sender.get
			if(tmp.isEmpty) {
				//Everything worked
				goto(Connected) 
			} else {
				//We still have answers to await for
				stay
			} using (map, tmp, finalDestination)
		}
		case Event(Timeout, _) => goto(Idle) using NullData
	}
	
	when(Connected) {
		case Ev(Disconnect) => goto(Idle) using NullData
	}
	
	onTransition {
		case Idle -> Dialing => setTimer("timeout", Timeout, 5 seconds, false)
		case Dialing -> AwaitingConfirmation => cancelTimer("timeout"); setTimer("timeout", Timeout, 5 seconds, false)
		case AwaitingConfirmation -> _ => cancelTimer("timeout")
		case _ -> Idle => cancelTimer("timeout");
	}
	
	initialize

}