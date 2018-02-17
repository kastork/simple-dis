package edu.nps.moves.simpledis

import io.vertx.core.net.NetSocket
import javafx.beans.binding.StringBinding
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

/**
 * A view model for the currently selected
 * service in the discovered services list of
 * the UI.
 *
 * This is a singleton that is injected by the TornadoFX
 * framework and so is, in a sense, global to the
 * running application.  Any TornadoFX component can ask for
 * this singleton by inject.
 *
 * The item in this model is a [Service] that is bound
 * whenever the UI list selection changes.
 */
class ServiceModel : ItemViewModel<Service>() {
	val serviceNameProperty = bind(Service::serviceName)
	val hostNameProperty = bind(Service::hostName)
	val addressProperty = bind(Service::address)
	val typeProperty = bind(Service::type)
	val portProperty = bind(Service::port)
	val socketProperty = bind(Service::socket)
	val indicatorProperty = bind(Service::indicator)
}

/**
 * Information about a service discovered
 * with mDNS.
 */
data class Service(val serviceName: String,
              val hostName: String,
              val address: String?,
              val type: String,
              val port: Int,
              var indicator: String = "-",
              var socket: NetSocket? = null) {
}
