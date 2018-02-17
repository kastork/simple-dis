package edu.nps.moves.simpledis

import tornadofx.*

class DisApp : App(MainView::class) {

	val networkController: NetworkController by inject()

	override fun stop() {
		networkController.closeServer()
		super.stop()
		System.exit(0)
	}
}


