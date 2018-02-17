package edu.nps.moves.simpledis

import tornadofx.*
import javax.jmdns.ServiceEvent

class ServerMessage(val message: String) : FXEvent()
class ServiceAdded(val service: Service) : FXEvent()
class ServiceResolved(val service: Service) : FXEvent()
class ServiceRemoved(val service: Service) : FXEvent()
