package edu.nps.moves.simpledis

import edu.nps.moves.dis.EntityStatePdu
import edu.nps.moves.disutil.PduFactory
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import javafx.application.Platform
import kotlinx.coroutines.experimental.async
import tornadofx.*
import java.net.Inet4Address
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

class NetworkController : Controller() {

	// TornadoFX view model that can be shared by
	// different components.  This one holds status
	// info useful for display in the UI
	val serverStatus: ServerStatus by inject()

	// TornadoFX view model that can be shared by
	// different components.  This one holds the
	// service that is currently selected in the
	// server list.
	val selectedService: ServiceModel by inject()

	// The vertx instance that provides all the async
	// networking capability (Netty and a bunch of
	// other stuff).
	val vertx = Vertx.vertx()

	// This server (the thing that can send PDUs)
	// must be started to be useful, and some client
	// (perhaps the one in this program) must subscribe
	// in order to receive sent PDUs.
	val server = vertx.createNetServer()

	// This client (the thing that can receive PDUs).
	// The client might connect to the same server in this
	// app, or one from another program.
	var client = vertx.createNetClient()

	// mDNS service -  advertises the server in this app
	// and finds other mDNS services on the network
	val jmDNS = JmDNS.create(Inet4Address.getLocalHost())

	// Things to put in the textarea (in lieu of printing
	// to console.)
	val serverMessages = mutableListOf<String>().observable()

	// All the services detected by mDNS query
	// i.e, servers that a client might choose to connect to
	val servers = mutableListOf<Service>().observable()

	// one socket for each client that has connected
	var serverSockets = mutableListOf<NetSocket>()

	// one socket for each client that has connected
	var clientSockets = mutableListOf<NetSocket>()


	init {
		/**
		 * What to do when a [ServerMessage] event is received.
		 */
		subscribe<ServerMessage> {
			// Any javaFX item that is observing the list
			// will update automatically
			serverMessages.add(it.message)
		}
		/**
		 * What to do when a [ServiceResolved] event is received.
		 */
		subscribe<ServiceResolved> {
			// Any javaFX item that is observing the list
			// will update automatically
			servers.add(it.service)
		}
		/**
		 * What to do when a [ServiceRemoved] is received.
		 */
		subscribe<ServiceRemoved> { event ->
			println("mDNS service removed: ${event.service}")
			val matchingService = servers.indexOfFirst { it.hostName == event.service.hostName && it.serviceName == event.service.serviceName }
			if (matchingService >= 0) {
				servers.removeAt(matchingService)
				Platform.runLater { servers.invalidate() }
			}
		}
		jmDNS.addServiceListener("_dis._tcp.local.", DisServiceListener())
	}

	/**
	 * Have the server in this program start listening for
	 * connections on a random port.
	 *
	 * If server startup is successful, then
	 *
	 * - Install a connect handler that adds the connection
	 *   socket to our list of PDU clients. [serverSockets]
	 * - Update the UI by incrementing the indicated client count
	 * - Install a handler that notices when a client closes the
	 *   connection and remove that connection from our list and
	 *   the indicated connection count in the UI.
	 * - Listen on localhost/0.0.0.0 on a random port
	 * - If listener setup succeeds, advertise on mDNS and update
	 *   the server port indicated in our UI.
	 * -
	 */
	fun startServer() {
		Platform.runLater { serverStatus.portProperty.value = -1 }
		server.connectHandler { socket ->
			println("Connection recieved on socket: ${socket}")
			serverSockets.add(socket)
			Platform.runLater {
				// since we're relying on javaFX observations, this must
				// be performed on the UI thread. Platform.runLater
				// does this for us.
				serverStatus.clientCountProperty.value = serverSockets.size
			}
			// What to do if the client that just connected closes its connection
			socket.closeHandler {
				serverSockets.remove(socket)
				Platform.runLater {
					serverStatus.clientCountProperty.value = serverSockets.size
				}
			}
			// in case some client sends us data (not something our
			// clients do, so seeing this would indicate something odd
			// going on.
			socket.handler { println("Server socket received data.") }
		}
		server.listen(0) {
			when {
				it.succeeded() -> {
					println("Server started, registering mDNS service")
					val p = server.actualPort()

					async { registerMdns() }
					Platform.runLater {
						serverStatus.portProperty.value = p
					}
				}

				it.failed() -> println("Failed")
			}
		}
	}

	/**
	 * Broadcast our service on mDNS.
	 *
	 * This is a Kotlin coroutine "suspending function"
	 * that returns immediately (before the work is done).
	 * We need to do this work on something other than the
	 * UI thread for JavaFX, but also not on a thread that
	 * would block the vertx event-thread.
	 *
	 * The update that takes place in the UI is done, as usual
	 * in the UI thread using Platform.runLater after the
	 * work is done.
	 */
	suspend fun registerMdns() {
		val port = serverStatus.port
		val serviceInfo =
				ServiceInfo.create("_dis._tcp.local.",
						"ESPDU-PRODUCER",
						port,
						"foo=bar")
		jmDNS.registerService(serviceInfo)
		val info = jmDNS.getServiceInfo("_dis._tcp.local.", "ESPDU-PRODUCER")
		Platform.runLater {
			serverStatus.serviceNameProperty.value = info.name ?: ""
		}
	}

	fun closeServer() {
		Platform.runLater {
			serverStatus.portProperty.value = -1
		}
		jmDNS.unregisterAllServices()
		server.close() {
			when {
				it.succeeded() -> {
					Platform.runLater {
						serverSockets.clear()
						serverStatus.portProperty.value = 0
						serverStatus.clientCountProperty.value = 0
					}
				}
			}
		}
	}

	fun sendPDU(): Unit {
		println("Sending some foo on ${serverSockets.size} connections.")

		val pdu = EntityStatePdu()
		with(pdu) {
			exerciseID = 1
			entityID.site = 1
			entityID.application = 1
			entityID.entity = 2
			timestamp += 1
			entityLocation.x = 1.0
			entityLocation.y = 1.0
			entityLocation.z = 1.0
		}
		val data = pdu.marshal()
		serverSockets.forEach { socket ->
			//buffers are not reusable
			val buffer = Buffer.buffer(data)
			socket.write(buffer)
		}
	}

	/**
	 * Subscribe to a PDU server.
	 *
	 * This could be the server in this application
	 * if it is started, or one running in another application.
	 *
	 * We only allow connecting to one server at a time.
	 *
	 * The information about which server to connect to comes
	 * from the shared [selectedService] object that is injected
	 * into this controller and the view by TornadoFX.  [selectedService]
	 * changes any time the user clicks a different item
	 */
	fun subscribeToPdus() {
		val service = selectedService.item
		println(service)
		if (service.socket != null) {
			return
		}
		println("Connecting to ${service.hostName}/${service.address}:${service.port}")
		client.connect(service.port, service.address) {
			when {
				it.succeeded() -> {
					val socket = it.result()
					println("Connection made $socket")

					socket.handler { buffer ->
						println("Client socket received data")
						val pdu = PduFactory().createPdu(buffer.bytes)
						println(pdu.toString())
						val pduText = when (pdu) {
							is EntityStatePdu ->
								"Entity State - Entity Category: ${pdu.entityType.category}, Location: (${pdu.entityLocation.x}, ${pdu.entityLocation.y}, ${pdu.entityLocation.z})"
							else -> "Unable to decode PDU"
						}

						fire(ServerMessage(pduText))
					}

					socket.closeHandler {
						println("${socket.remoteAddress()} closed.")
					}
					service.socket = socket
					service.indicator = "+"
					println("Updated UI: ${service.indicator} ")
					Platform.runLater { servers.invalidate() }
				}
				it.failed() -> {
					println("Connection failed ${it.cause().message}")
				}
			}
		}
	}

	/**
	 * Unsubscribe our client from the server in [selectedService]
	 * (the one currently selected in the UI.)
	 */
	fun unsubscribeFromPdus() {
		val service = selectedService.item
		println("Disconnecting from ${service.hostName}/${service.address}:${service.port}")
		val socket = service.socket
		if (socket != null) {
			socket.close()
		}
		Platform.runLater {
			selectedService.indicatorProperty.value = "-"
			selectedService.commit()
			servers.invalidate()
		}
	}

	/**
	 * A [ServiceListener] that merely retransmits the useful
	 * data from an mDNS Service Event to a TornadoFX Event
	 * that components can subscribe to.
	 *
	 * This way we don't have to keep any state references
	 * from the controller or view here in this object.
	 */
	private class DisServiceListener : ServiceListener {

		override fun serviceResolved(event: ServiceEvent) {
			val s = Service(
					event.name,
					event.dns.inetAddress.hostName,
					event.info.inet4Addresses[0].hostAddress,
					event.type,
					event.info.port)

			FX.eventbus.fire(ServiceResolved(s))
		}

		override fun serviceRemoved(event: ServiceEvent) {
			println("SERVICE REMOVED: ${event.info}")
			println(event.dns.inetAddress.hostName)
			println(event.type)
			println(event.info.port)
//			println(event.info.inet4Addresses[0]?.hostAddress)

			val s = Service(
					event.name,
					event.dns.inetAddress.hostName,
					null,
					event.type,
					event.info.port)

			println(s)
			FX.eventbus.fire(ServiceRemoved(s))
		}

		override fun serviceAdded(event: ServiceEvent) {
//			val s = Service(
//					event.dns.inetAddress.hostName,
//					event.info.inet4Addresses[0].hostAddress,
//					event.type,
//					event.info.port)
//			FX.eventbus.fire(ServiceAdded(s))
		}
	}

}

