package multilink.util

import multilink.util.replication.ReplicatableActor
import multilink.util.composition.ComposableActor

trait MultilinkActor[State] extends ComposableActor with ReplicatableActor[State]{

}