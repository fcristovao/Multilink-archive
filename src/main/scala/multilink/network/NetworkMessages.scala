package multilink.network

sealed trait NetworkMessages
case object Disable extends NetworkMessages
case object Bypass extends NetworkMessages
case object Activate extends NetworkMessages