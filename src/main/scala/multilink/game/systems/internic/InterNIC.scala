package multilink.game.systems.internic

import multilink.game.network.intranet.Gateway
import multilink.game.network.internet._
import multilink.game.network.intranet.AccessControl.Credentials
import multilink.game.systems.internic.InterNICWebServer.AddressBook


object InterNIC {
  import multilink.util.composition.ArrowOperator._

  case class Config(ip: InternetPointAddress,
                    addressBook: AddressBook,
                    credentials: Credentials)

  def apply(config: Config) = {
    Gateway(config.ip) >>> InterNICWebServer(config.addressBook)
  }
}

