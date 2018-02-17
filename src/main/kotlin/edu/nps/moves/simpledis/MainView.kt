package edu.nps.moves.simpledis

import javafx.geometry.Orientation
import javafx.scene.control.ListView
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import tornadofx.*

class MainView : View() {
	val networkController: NetworkController by inject()
	val selectedService: ServiceModel by inject()
	val status: ServerStatus by inject()
	var messageArea: TextArea by singleAssign()
	var serviceList: ListView<Service> by singleAssign()

	init {
	}

	override fun onDock() {
		networkController.serverMessages.onChange {
			while (it.next()) {
				it.addedSubList.forEach { messageArea.text += "$it\n" }
			}
		}
	}

	override val root = borderpane {
		top {

		}
		center {
			hbox {
				vbox(20) {
					form {
						fieldset("Server Status") {
							field("DIS Port:") {
								label(status.portProperty)
							}
							field("Connected Clients: ") {
								label(status.clientCountProperty)
							}
							buttonbar {
								button("Start Server") {
									action {
										runAsync {
											networkController.startServer()
										}
									}
									enableWhen(status.portProperty.eq(0))
								}
								button("Stop Server") {
									action {
										runAsync {
											networkController.closeServer()
										}
									}
									enableWhen(status.portProperty.greaterThan(0))
								}
								button("Send PDU") {
									action {
										runAsync { networkController.sendPDU() }
									}
									enableWhen(status.clientCountProperty.greaterThan(0))
								}
							}
						}
						separator(Orientation.HORIZONTAL)
//					}
//				}
//				vbox(20) {
//				form {
						fieldset("Client Status", labelPosition = Orientation.VERTICAL) {
							field("Available Servers") {
								serviceList = listview(networkController.servers) {
									cellFormat {
										text = "${it.indicator} ${it.address}:${it.port} ${it.serviceName}"
									}
									bindSelected(selectedService)
								}
							}
							buttonbar {
								button("Unsubscribe from PDUs") {
									action {
										runAsync {
											networkController.unsubscribeFromPdus()
										}
									}
									enableWhen(selectedService
										.itemProperty
										.isNotNull
										.and(selectedService.indicatorProperty.booleanBinding { it == "+" }))
								}
								button("Subscribe to PDUs") {
									action {
										runAsync {
											networkController.subscribeToPdus()
//											ui {
//												serviceList.selectionModel.select(selectedService.item)
//											}
										}
									}
									enableWhen(selectedService
										.itemProperty
										.isNotNull
										.and(selectedService.indicatorProperty.booleanBinding { it == "-" }))
								}
							}
						}
					}
				}
				form {
					fieldset("Messages", labelPosition = Orientation.VERTICAL) {
					}
					vbox {
						vgrow = Priority.ALWAYS
						messageArea = textarea() {
							usePrefHeight = true
							vgrow = Priority.ALWAYS
						}
					}
				}

			}
		}
		bottom {
		}
	}

}