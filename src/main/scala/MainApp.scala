import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.{Label, MenuBar, Menu, MenuItem}
import scalafx.scene.layout.{BorderPane, VBox}
import scalafx.geometry.{Insets, Pos}

object MainApp extends JFXApp3:

  override def start(): Unit =

    // ── Placeholder screens (one VBox per section) ──
    val inventoryScreen = new VBox:
      padding = Insets(20)
      alignment = Pos.Center
      children = Seq(new Label("Inventory Screen"))

    val beneficiaryScreen = new VBox:
      padding = Insets(20)
      alignment = Pos.Center
      children = Seq(new Label("Beneficiaries Screen"))

    val smartMatchScreen = new VBox:
      padding = Insets(20)
      alignment = Pos.Center
      children = Seq(new Label("Smart Match Screen"))

    val alertsExportScreen = new VBox:
      padding = Insets(20)
      alignment = Pos.Center
      children = Seq(new Label("Alerts / Export Screen"))

    // ── Root layout ──
    val rootPane = new BorderPane

    // ── Menu bar with 4 sections ──
    val inventoryItem = new MenuItem("Inventory"):
      onAction = handle { rootPane.center = inventoryScreen }

    val beneficiaryItem = new MenuItem("Beneficiaries"):
      onAction = handle { rootPane.center = beneficiaryScreen }

    val smartMatchItem = new MenuItem("Smart Match"):
      onAction = handle { rootPane.center = smartMatchScreen }

    val alertsExportItem = new MenuItem("Alerts / Export"):
      onAction = handle { rootPane.center = alertsExportScreen }

    val navigateMenu = new Menu("Navigate"):
      items = Seq(inventoryItem, beneficiaryItem, smartMatchItem, alertsExportItem)

    val menuBar = new MenuBar:
      menus = Seq(navigateMenu)

    rootPane.top = menuBar
    rootPane.center = inventoryScreen // default screen

    stage = new JFXApp3.PrimaryStage:
      title = "Food Pantry Management System"
      width = 900
      height = 600
      scene = new Scene:
        root = rootPane
