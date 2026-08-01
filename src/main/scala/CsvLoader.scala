import com.github.tototoshi.csv.CSVReader
import scala.util.Try
import java.io.File

object CsvLoader:

  def loadInventory(path: String): Try[List[PantryItem]] =
    Try:
      val reader = CSVReader.open(new File(path))
      try
        val rows = reader.allWithHeaders()
        rows.map: row =>
          val id     = row("Item_ID")
          val name   = row("Item_Name")
          val qty    = row("Stock_Quantity").toInt
          val expiry = row("Expiration_Date")

          row("Type") match
            case "Perishable" => Perishable(id, name, qty, expiry, false)
            case _            => NonPerishable(id, name, qty, expiry)
      finally
        reader.close()

  def loadBeneficiaries(path: String): Try[List[Beneficiary]] =
    Try:
      val reader = CSVReader.open(new File(path))
      try
        val rows = reader.allWithHeaders()
        rows.map: row =>
          Beneficiary(
            row("Family_ID"),
            row("Family_Name"),
            row("Size").toInt,
            row("Requested_Food"),
            row("Date_Requested")
          )
      finally
        reader.close()

  def loadExports(path: String): Try[List[Map[String, String]]] =
    Try:
      val reader = CSVReader.open(new File(path))
      try
        reader.allWithHeaders()
      finally
        reader.close()
