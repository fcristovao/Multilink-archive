package multilink.client

import scala.swing._

object Client extends SimpleSwingApplication { 
	def top = new MainFrame {
		title = "First Swing App" 
		contents = new Button {
			text = "Click me"
		}
	}
}