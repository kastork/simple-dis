package edu.nps.moves.simpledis

import io.vertx.core.net.NetSocket
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

/**
 * Simple ViewModel containing observable properties
 * used in the UI.
 *
 * Here we keep track of the port of our server,
 * services discovered using mDNS and our own server's
 * client connection count.
 *
 * If you update a property from the UI thread, then
 * any UI element that is bound to that property will
 * update automatically.
 */
class ServerStatus(port: Int? = null, clientCount: Int? = null,
                   serviceName: String = ""
) : ViewModel() {
	val portProperty = SimpleIntegerProperty()
	var port by portProperty

	val clientCountProperty = SimpleIntegerProperty()
	var clientCount by clientCountProperty

	val serviceNameProperty = SimpleStringProperty()
	var serviceName by serviceNameProperty

	init {

	}
}