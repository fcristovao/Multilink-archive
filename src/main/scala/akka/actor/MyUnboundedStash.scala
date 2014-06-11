package akka.actor

trait MyUnboundedStash extends UnboundedStash{

  override def unstash() = super.unstash()
}
