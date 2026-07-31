import scala.util.Try
import java.io.PrintWriter

trait DataExporter[T]:
  def exportData(data: List[T], path: String): Try[Unit]

object ReceiptExporter extends DataExporter[(Beneficiary, PantryItem)]:

  override def exportData(data: List[(Beneficiary, PantryItem)], path: String): Try[Unit] =
    Try:
      val writer = new PrintWriter(path)
      try
        data.foreach: (beneficiary, item) =>
          writer.println(s"Family ${beneficiary.familyName} received ${item.name} (${item.itemId})")
      finally
        writer.close()
