import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.{Label, Button, TableView, TableColumn, TextField, TableCell}
import scalafx.scene.layout.{BorderPane, VBox, HBox}
import scalafx.geometry.{Insets, Pos}
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.{StringProperty, ObjectProperty}

object MainApp extends JFXApp3:

  override def start(): Unit =

    // ── Load Data ──
    var currentInventory = CsvLoader.loadInventory("src/main/resources/PantryItem.csv").getOrElse(List.empty)
    var currentBeneficiaries = CsvLoader.loadBeneficiaries("src/main/resources/Beneficiary.csv").getOrElse(List.empty)
    
    val inventoryBuffer = ObservableBuffer.from(currentInventory)
    val beneficiaryBuffer = ObservableBuffer.from(currentBeneficiaries)


    // ── Inventory Screen ──
    val inventoryTable = new TableView[PantryItem](inventoryBuffer):
      columns ++= Seq(
        new TableColumn[PantryItem, String]("Item ID") {
          cellValueFactory = cell => StringProperty(cell.value.itemId)
        },
        new TableColumn[PantryItem, String]("Name") {
          cellValueFactory = cell => StringProperty(cell.value.name)
        },
        new TableColumn[PantryItem, Int]("Quantity") {
          cellValueFactory = cell => ObjectProperty(cell.value.quantity)
        },
        new TableColumn[PantryItem, String]("Expiry Date") {
          cellValueFactory = cell => StringProperty(cell.value.expiryDate)
        },
        new TableColumn[PantryItem, String]("Type") {
          cellValueFactory = cell => StringProperty(
            cell.value match
              case _: Perishable => "Perishable"
              case _: NonPerishable => "NonPerishable"
          )
        }
      )

    val inventoryScreen = new VBox(10):
      padding = Insets(20)
      alignment = Pos.TopCenter
      children = Seq(
        new Label("Pantry Inventory") { style = "-fx-font-size: 20pt; -fx-font-weight: bold;" },
        inventoryTable
      )

    // ── Beneficiaries Screen ──
    val beneficiaryTable = new TableView[Beneficiary](beneficiaryBuffer):
      columns ++= Seq(
        new TableColumn[Beneficiary, String]("Family ID") {
          cellValueFactory = cell => StringProperty(cell.value.familyId)
        },
        new TableColumn[Beneficiary, String]("Family Name") {
          cellValueFactory = cell => StringProperty(cell.value.familyName)
        },
        new TableColumn[Beneficiary, Int]("Size") {
          cellValueFactory = cell => ObjectProperty(cell.value.size)
        },
        new TableColumn[Beneficiary, String]("Requested Food") {
          cellValueFactory = cell => StringProperty(cell.value.requestedFood)
        },
        new TableColumn[Beneficiary, String]("Date Requested") {
          cellValueFactory = cell => StringProperty(cell.value.dateRequested)
        }
      )

      // view
    val beneficiaryScreen = new VBox(10):
      padding = Insets(20)
      alignment = Pos.TopCenter
      children = Seq(
        new Label("Beneficiary Requests") { style = "-fx-font-size: 20pt; -fx-font-weight: bold;" },
        beneficiaryTable
      )

    // ── Smart Match Screen ──
    case class MatchRecord(beneficiary: Beneficiary, item: PantryItem, confirmed: ObjectProperty[Boolean] = ObjectProperty(false))
    
    val matchBuffer = ObservableBuffer.empty[MatchRecord]
    
    val matchTable = new TableView[MatchRecord](matchBuffer):
      columns ++= Seq(
        new TableColumn[MatchRecord, String]("Family Name") {
          cellValueFactory = cell => StringProperty(cell.value.beneficiary.familyName)
        },
        new TableColumn[MatchRecord, String]("Allocated Item") {
          cellValueFactory = cell => StringProperty(cell.value.item.name)
        },
        new TableColumn[MatchRecord, String]("Quantity") {
          cellValueFactory = cell => StringProperty(cell.value.beneficiary.size.toString)
        },
        new TableColumn[MatchRecord, String]("Date Requested") {
          cellValueFactory = cell => StringProperty(cell.value.beneficiary.dateRequested)
        },
        new TableColumn[MatchRecord, String]("Type") {
          cellValueFactory = cell => StringProperty(if cell.value.item.isInstanceOf[Perishable] then "Perishable" else "NonPerishable")
        },
        new TableColumn[MatchRecord, String]("Munual Check") {
          // ai-assisted: #9
          // why: Boilerplate for custom TableCell creation in ScalaFX is complex and unintuitive.
          /**
           * if the item is perishable
           * once user confirmed
           * it call update function
           *  copying the old data but explicitly setting its expired status (the false at the end) to false.
           */
          cellValueFactory = _ => StringProperty("")
          cellFactory = (_: TableColumn[MatchRecord, String]) => new TableCell[MatchRecord, String] {
            item.onChange { (_, _, _) =>
              val row = tableRow.value
              if (row != null && row.item.value != null) {
                val record = row.item.value
                if (record.item.isInstanceOf[Perishable]) {
                  val btn = new Button("Confirm Not Expired")
                  btn.onAction = handle {
                    record.confirmed.value = true
                    btn.text = "Confirmed"
                    btn.disable = true
                    val p = record.item.asInstanceOf[Perishable]
                    val confirmedItem = Perishable(p.itemId, p.name, p.quantity, p.expiryDate, false)
                    currentInventory = Manage.update(currentInventory, p.itemId, confirmedItem)
                    inventoryBuffer.clear()
                    inventoryBuffer ++= currentInventory
                    Manage.savePantryItems(currentInventory, "src/main/resources/PantryItem.csv")
                  }
                  if (record.confirmed.value) {
                    btn.text = "Confirmed"
                    btn.disable = true
                  }
                  graphic = btn
                } else {
                  graphic = new Label("N/A")
                }
              } else {
                graphic = null
              }
            }
          }
        }
      )

    // SmartMatch button and text field button

    val dateInput = new TextField { promptText = "Enter Date (d/M/yyyy)"; prefWidth = 200 }

    val executeErrorLabel = new Label("")
    executeErrorLabel.style = "-fx-font-size: 14pt; -fx-text-fill: red;"

    val runMatchBtn = new Button("Run Smart Match"):
      style = "-fx-font-size: 16pt;"
      onAction = handle {
        val today = dateInput.text.value.trim
        val isInvalidDate = (d: String) => scala.util.Try(java.time.LocalDate.parse(d, java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"))).isFailure
        if (today.nonEmpty) {
          if (isInvalidDate(today)) {
            executeErrorLabel.text = "Error: Date must be d/M/yyyy format." // prevent error
          } else {
            try {
              val (allocated, _) = SmartMatcher.allocateFood(currentInventory, currentBeneficiaries, today)
              val records = allocated.map { case (b, i) => MatchRecord(b, i) }
              matchBuffer.clear()
              matchBuffer ++= records
              executeErrorLabel.text = ""
            } catch {
              case _: Exception => // ignore invalid date format
            }
          }
        }
      }

    val executeDateInput = new TextField { promptText = "Enter Execute Date (d/M/yyyy)"; prefWidth = 250 }

    val executeBtn = new Button("Execute"):
      // ai-assisted: #10
      // why: The execution logic requires intricate state management, file I/O operations, and model conversions.
      style = "-fx-font-size: 16pt;"
      onAction = handle {
        val execDate = executeDateInput.text.value.trim
        val isInvalidDate = (d: String) => scala.util.Try(java.time.LocalDate.parse(d, java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"))).isFailure
        if (execDate.isEmpty) {
          executeErrorLabel.text = "Error: Please enter an execute date."
        } else if (isInvalidDate(execDate)) {
          executeErrorLabel.text = "Error: Date must be d/M/yyyy format."
        } else if (matchBuffer.isEmpty) {
          executeErrorLabel.text = "Error: No items to execute."
        } else {
          val hasUnconfirmed = matchBuffer.exists { r =>
            r.item.isInstanceOf[Perishable] && !r.confirmed.value
          }
          if (hasUnconfirmed) {
            executeErrorLabel.text = "Error: Please confirm all perishable items first."
          } else {
            executeErrorLabel.text = ""
            val pairs = matchBuffer.toList.map(r => (r.beneficiary, r.item))
            // write to ExportItem.csv
            SmartMatcher.exportAllocations(pairs, execDate, "src/main/resources/ExportItem.csv")
            // delete matched beneficiaries from list
            matchBuffer.foreach { r =>
              currentBeneficiaries = Manage.delete(currentBeneficiaries, r.beneficiary.familyId)
            }
            beneficiaryBuffer.clear()
            beneficiaryBuffer ++= currentBeneficiaries
            Manage.saveBeneficiaries(currentBeneficiaries, "src/main/resources/Beneficiary.csv")
            // deduct quantity from inventory
            matchBuffer.foreach { r =>
              val newQty = r.item.quantity - r.beneficiary.size
              val updatedItem: PantryItem = r.item match
                case p: Perishable => Perishable(p.itemId, p.name, newQty, p.expiryDate, p.isSpoiledPrematurely)
                case np: NonPerishable => NonPerishable(np.itemId, np.name, newQty, np.expiryDate)
              currentInventory = Manage.update(currentInventory, r.item.itemId, updatedItem)
            }
            inventoryBuffer.clear()
            inventoryBuffer ++= currentInventory
            Manage.savePantryItems(currentInventory, "src/main/resources/PantryItem.csv")
            // clear table
            matchBuffer.clear()
            executeErrorLabel.style = "-fx-font-size: 14pt; -fx-text-fill: green;"
            executeErrorLabel.text = "Execution successful!"
          }
        }
      }

    val smartMatchScreen = new VBox(10):
      padding = Insets(20)
      alignment = Pos.TopCenter
      children = Seq(
        new Label("Smart Match Allocations") { style = "-fx-font-size: 20pt; -fx-font-weight: bold;" },
        new HBox(10) { alignment = Pos.Center; children = Seq(new Label("Simulation Date:"), dateInput, runMatchBtn) },
        matchTable,
        new HBox(10) { alignment = Pos.Center; children = Seq(new Label("Execute Date:"), executeDateInput, executeBtn) },
        executeErrorLabel
      )
      
    // Export
    case class ExportRecord(familyId: String, itemId: String, size: String, executeDate: String)
    val exportBuffer = ObservableBuffer.empty[ExportRecord]

    val exportTable = new TableView[ExportRecord](exportBuffer):
      columns ++= Seq(
        new TableColumn[ExportRecord, String]("Family ID") {
          cellValueFactory = cell => StringProperty(cell.value.familyId)
        },
        new TableColumn[ExportRecord, String]("Item ID") {
          cellValueFactory = cell => StringProperty(cell.value.itemId)
        },
        new TableColumn[ExportRecord, String]("Size") {
          cellValueFactory = cell => StringProperty(cell.value.size)
        },
        new TableColumn[ExportRecord, String]("Execute Date") {
          cellValueFactory = cell => StringProperty(cell.value.executeDate)
        }
      )
      
    val loadExportsBtn = new Button("Load Exports"):
      style = "-fx-font-size: 16pt;"
      onAction = handle {
        val loaded = CsvLoader.loadExports("src/main/resources/ExportItem.csv").getOrElse(List.empty)
        val records = loaded.map(row => 
          ExportRecord(
            row.getOrElse("Family_ID", ""),
            row.getOrElse("Item_ID", ""),
            row.getOrElse("Size", ""),
            row.getOrElse("Execute_Date", "")
          )
        )
        exportBuffer.clear()
        exportBuffer ++= records
      }

    val alertsExportScreen = new VBox(10):
      padding = Insets(20)
      alignment = Pos.TopCenter
      children = Seq(
        new Label("Exported Records") { style = "-fx-font-size: 20pt; -fx-font-weight: bold;" },
        loadExportsBtn,
        exportTable
      )

    // ── Root layout ──
    val rootPane = new BorderPane

    // ── View Selection Screen ──
    val viewSelectionScreen = new VBox(20):
      padding = Insets(50)
      alignment = Pos.Center
      children = Seq(
        new Label("Select Table View") { style = "-fx-font-size: 20pt; -fx-font-weight: bold;" },
        new Button("Pantry") {
          style = "-fx-font-size: 16pt; -fx-min-width: 200px;"
          onAction = handle { rootPane.center = inventoryScreen }
        },
        new Button("Beneficiary") {
          style = "-fx-font-size: 16pt; -fx-min-width: 200px;"
          onAction = handle { rootPane.center = beneficiaryScreen }
        }
      )

    
    // ── Manage Screens ──
    val formScreen = new VBox(15):
      padding = Insets(50)
      alignment = Pos.Center

    val crudActionScreen = new VBox(20):
      padding = Insets(50)
      alignment = Pos.Center

    def showForm(entityType: String, action: String): Unit =
      // ai-assisted: #11
      // why: Dynamically generating a GUI form based on entity types involves lengthy conditional validation and mapping.
      formScreen.children.clear()
      val title = new Label(s"$action $entityType") { style = "-fx-font-size: 16pt; -fx-font-weight: bold;" }
      
      val inputContainer = new VBox(10)
      inputContainer.alignment = Pos.Center
      
      var textFields = Map.empty[String, TextField]
      
      val fields = action match
        case "Create" | "Update" =>
          if entityType == "Pantry Item" then Seq("ID", "Name", "Qty", "Expiry", "Type (Perishable/NonPerishable)")
          else Seq("FamilyID", "Name", "Size", "RequestedFood", "Date")
        case "Read" | "Delete" =>
          Seq("ID")
          
      fields.foreach { f =>
        val tf = new TextField { promptText = f; prefWidth = 400 }
        textFields += (f -> tf)
        inputContainer.children.add(tf)
      }
      
      val confirmBtn = new Button("Confirm")
      val resultLabel = new Label("")
      resultLabel.style = "-fx-font-size: 16pt; -fx-font-weight: bold;"
      
      confirmBtn.onAction = handle {
        try
          if fields.exists(f => textFields(f).text.value.trim.isEmpty) then
            resultLabel.text = "Error: Must fill in all fields!"
          else
            if entityType == "Pantry Item" then
              val qtyField = textFields.get("Qty").map(_.text.value.trim)
              val typeField = textFields.get("Type (Perishable/NonPerishable)").map(_.text.value.trim)
              val expiryField = textFields.get("Expiry").map(_.text.value.trim)
              
              val isInvalidQty = (action == "Create" || action == "Update") && qtyField.exists(q => q.toIntOption.isEmpty || q.toInt < 0)
              val isInvalidType = (action == "Create" || action == "Update") && typeField.exists(t => t != "Perishable" && t != "NonPerishable")
              val isInvalidDate = (action == "Create" || action == "Update") && expiryField.exists(d => scala.util.Try(java.time.LocalDate.parse(d, java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"))).isFailure)
              
              if isInvalidQty then resultLabel.text = "Error: Qty must be a positive number."
              else if isInvalidType then resultLabel.text = "Error: Type must be Perishable or NonPerishable."
              else if isInvalidDate then resultLabel.text = "Error: Expiry must be d/M/yyyy format."
              else
                action match
                case "Create" =>
                  val isPerishable = textFields("Type (Perishable/NonPerishable)").text.value.trim == "Perishable"
                  val item = if isPerishable then Perishable(textFields("ID").text.value.trim, textFields("Name").text.value.trim, textFields("Qty").text.value.trim.toInt, textFields("Expiry").text.value.trim, false) else NonPerishable(textFields("ID").text.value.trim, textFields("Name").text.value.trim, textFields("Qty").text.value.trim.toInt, textFields("Expiry").text.value.trim)
                  currentInventory = Manage.create(currentInventory, item)
                  inventoryBuffer.clear()
                  inventoryBuffer ++= currentInventory
                  Manage.savePantryItems(currentInventory, "src/main/resources/PantryItem.csv")
                  resultLabel.text = "Item Created!"
                case "Read" =>
                  val item = Manage.read(currentInventory, textFields("ID").text.value.trim)
                  resultLabel.text = item.map(i => s"${i.itemId} - ${i.name} - ${i.quantity} - ${i.expiryDate}").getOrElse("Not Found")
                case "Update" =>
                  val isPerishable = textFields("Type (Perishable/NonPerishable)").text.value.trim == "Perishable"
                  val item = if isPerishable then Perishable(textFields("ID").text.value.trim, textFields("Name").text.value.trim, textFields("Qty").text.value.trim.toInt, textFields("Expiry").text.value.trim, false) else NonPerishable(textFields("ID").text.value.trim, textFields("Name").text.value.trim, textFields("Qty").text.value.trim.toInt, textFields("Expiry").text.value.trim)
                  currentInventory = Manage.update(currentInventory, textFields("ID").text.value.trim, item)
                  inventoryBuffer.clear()
                  inventoryBuffer ++= currentInventory
                  Manage.savePantryItems(currentInventory, "src/main/resources/PantryItem.csv")
                  resultLabel.text = "Item Updated!"
                case "Delete" =>
                  currentInventory = Manage.delete(currentInventory, textFields("ID").text.value.trim)
                  inventoryBuffer.clear()
                  inventoryBuffer ++= currentInventory
                  Manage.savePantryItems(currentInventory, "src/main/resources/PantryItem.csv")
                  resultLabel.text = "Item Deleted!"
            else
              val sizeField = textFields.get("Size").map(_.text.value.trim)
              val dateField = textFields.get("Date").map(_.text.value.trim)
              
              val isInvalidSize = (action == "Create" || action == "Update") && sizeField.exists(s => s.toIntOption.isEmpty || s.toInt <= 0)
              val isInvalidDate = (action == "Create" || action == "Update") && dateField.exists(d => scala.util.Try(java.time.LocalDate.parse(d, java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"))).isFailure)
              
              if isInvalidSize then resultLabel.text = "Error: Size must be a positive number."
              else if isInvalidDate then resultLabel.text = "Error: Date must be d/M/yyyy format."
              else
                action match
                case "Create" =>
                  val item = Beneficiary(textFields("FamilyID").text.value.trim, textFields("Name").text.value.trim, textFields("Size").text.value.trim.toInt, textFields("RequestedFood").text.value.trim, textFields("Date").text.value.trim)
                  currentBeneficiaries = Manage.create(currentBeneficiaries, item)
                  beneficiaryBuffer.clear()
                  beneficiaryBuffer ++= currentBeneficiaries
                  Manage.saveBeneficiaries(currentBeneficiaries, "src/main/resources/Beneficiary.csv")
                  resultLabel.text = "Beneficiary Created!"
                case "Read" =>
                  val item = Manage.read(currentBeneficiaries, textFields("ID").text.value.trim)
                  resultLabel.text = item.map(i => s"${i.familyId} - ${i.familyName} - ${i.requestedFood} - ${i.dateRequested}").getOrElse("Not Found")
                case "Update" =>
                  val item = Beneficiary(textFields("FamilyID").text.value.trim, textFields("Name").text.value.trim, textFields("Size").text.value.trim.toInt, textFields("RequestedFood").text.value.trim, textFields("Date").text.value.trim)
                  currentBeneficiaries = Manage.update(currentBeneficiaries, textFields("FamilyID").text.value.trim, item)
                  beneficiaryBuffer.clear()
                  beneficiaryBuffer ++= currentBeneficiaries
                  Manage.saveBeneficiaries(currentBeneficiaries, "src/main/resources/Beneficiary.csv")
                  resultLabel.text = "Beneficiary Updated!"
                case "Delete" =>
                  currentBeneficiaries = Manage.delete(currentBeneficiaries, textFields("ID").text.value.trim)
                  beneficiaryBuffer.clear()
                  beneficiaryBuffer ++= currentBeneficiaries
                  Manage.saveBeneficiaries(currentBeneficiaries, "src/main/resources/Beneficiary.csv")
                  resultLabel.text = "Beneficiary Deleted!"
        catch
          case _: Exception => resultLabel.text = s"Error: Invalid input format."
      }
      
      val backBtn = new Button("Back") { onAction = handle { rootPane.center = crudActionScreen } }
      
      formScreen.children = Seq(title, inputContainer, confirmBtn, resultLabel, backBtn)
      rootPane.center = formScreen

    def showCrudScreen(entityType: String): Unit =
      crudActionScreen.children.clear()
      val title = new Label(s"Manage $entityType") { style = "-fx-font-size: 20pt; -fx-font-weight: bold;" }
      val btnBox = new HBox(10):
        alignment = Pos.Center
        children = Seq(
          new Button("Create") { onAction = handle { showForm(entityType, "Create") } },
          new Button("Read") { onAction = handle { showForm(entityType, "Read") } },
          new Button("Update") { onAction = handle { showForm(entityType, "Update") } },
          new Button("Delete") { onAction = handle { showForm(entityType, "Delete") } }
        )
      val backBtn = new Button("Back") { onAction = handle { rootPane.center = manageSelectionScreen } }
      crudActionScreen.children = Seq(title, btnBox, backBtn)
      rootPane.center = crudActionScreen

    lazy val manageSelectionScreen: VBox = new VBox(20):
      padding = Insets(50)
      alignment = Pos.Center
      children = Seq(
        new Label("Select Table to Manage") { style = "-fx-font-size: 20pt; -fx-font-weight: bold;" },
        new Button("Pantry") {
          style = "-fx-font-size: 16pt; -fx-min-width: 200px;"
          onAction = handle { showCrudScreen("Pantry Item") }
        },
        new Button("Beneficiary") {
          style = "-fx-font-size: 16pt; -fx-min-width: 200px;"
          onAction = handle { showCrudScreen("Beneficiary") }
        }
      )

    // ── Button bar with new Manage button ──
    val viewBtn = new Button("View"):
      onAction = handle { rootPane.center = viewSelectionScreen }

    val manageBtn = new Button("Manage"):
      onAction = handle { rootPane.center = manageSelectionScreen }

    val smartMatchBtn = new Button("Smart Match"):
      onAction = handle { rootPane.center = smartMatchScreen }

    val alertsExportBtn = new Button("Alerts / Export"):
      onAction = handle { rootPane.center = alertsExportScreen }

    val buttonBar = new HBox(10):
      padding = Insets(10)
      alignment = Pos.Center
      children = Seq(viewBtn, manageBtn, smartMatchBtn, alertsExportBtn)

    rootPane.top = buttonBar
    rootPane.center = viewSelectionScreen // default screen

    stage = new JFXApp3.PrimaryStage:
      title = "Food Pantry Management System"
      width = 900
      height = 600
      scene = new Scene:
        root = rootPane
