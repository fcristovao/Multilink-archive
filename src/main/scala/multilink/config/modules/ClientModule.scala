package multilink.config.modules

import scaldi.{Injectable, Module}
import multilink.game.client.{Client}


class ClientModule extends Module with Injectable {

  //bind[Client] to new Client()
}
