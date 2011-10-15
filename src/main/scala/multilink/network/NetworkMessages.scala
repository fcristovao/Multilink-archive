package multilink.network



sealed trait NetworkMessages

sealed trait FirewallMessages
sealed trait GatewayMessages

case object DisableFirewall extends NetworkMessages with FirewallMessages 
case object BypassFirewall extends NetworkMessages with FirewallMessages
case object EnableFirewall extends NetworkMessages with FirewallMessages 

case object DisableGateway extends NetworkMessages with GatewayMessages
case object EnableGateway extends NetworkMessages with GatewayMessages