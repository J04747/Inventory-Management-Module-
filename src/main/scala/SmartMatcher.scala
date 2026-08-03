import java.time.LocalDate
import java.time.format.DateTimeFormatter

object SmartMatcher:

  // parse dates in either "d/M/yyyy" (CSV) or "yyyy-MM-dd" (ISO) format
  private val csvDateFormat = DateTimeFormatter.ofPattern("d/M/yyyy")

  private def parseDate(dateStr: String): LocalDate =
    try LocalDate.parse(dateStr) // try ISO format first
    catch case _: Exception => LocalDate.parse(dateStr, csvDateFormat)

  /** Pairs eligible beneficiaries with available inventory items using pure FP.
    * Beneficiaries whose dateRequested <= today are eligible for allocation.
    * Beneficiaries whose dateRequested > today are placed on a waiting list.
    * Items are allocated based on their quantity – the same item can be
    * given to multiple beneficiaries until its quantity reaches 0.
    * Returns (allocated pairs, waiting list).
    */
  // ai-assisted: #6
  // why: Implemented pure FP list partitioning based on temporal constraints.
  def allocateFood[T <: PantryItem](
    inventory: List[T],
    requests: List[Beneficiary],
    today: String
  ): (List[(Beneficiary, T)], List[Beneficiary]) =

    val todayDate = parseDate(today)

    // split into eligible (date arrived) and waiting (date not yet arrived)
    val (eligible, waiting) = requests.partition: b =>
      !parseDate(b.dateRequested).isAfter(todayDate) // dateRequested <= today

    // build an immutable map of itemId -> remaining quantity
    val initialStock: Map[String, Int] =
      inventory.map(item => item.itemId -> item.quantity).toMap

    // ai-assisted: #7
    // why: Utilized foldLeft to maintain immutable state of stock and allocations across iterations
    // ── Pass 1: pair eligible beneficiaries whose requested item name matches ──
    val (nameMatched, stockAfterPass1, unmatchedRequests) =
      eligible.foldLeft((List.empty[(Beneficiary, T)], initialStock, List.empty[Beneficiary])):
        case ((paired, stock, unmatched), beneficiary) =>
          // find an item whose name matches AND has enough quantity for the family
          inventory.find(item =>
            item.name.equalsIgnoreCase(beneficiary.requestedFood) &&
            stock.getOrElse(item.itemId, 0) >= beneficiary.size
          ) match
            case Some(item) =>
              // pair them and decrement by family size
              val updatedStock = stock.updated(item.itemId, stock(item.itemId) - beneficiary.size)
              ((beneficiary, item) :: paired, updatedStock, unmatched)
            case None =>
              // no name match found – save for Pass 2
              (paired, stock, beneficiary :: unmatched)

    // ── Pass 2: pair remaining eligible beneficiaries with leftover items in order ──
    val (orderMatched, _) =
      unmatchedRequests.reverse.foldLeft((List.empty[(Beneficiary, T)], stockAfterPass1)):
        case ((paired, stock), beneficiary) =>
          // find the first inventory item with enough quantity for the family
          inventory.find(item => stock.getOrElse(item.itemId, 0) >= beneficiary.size) match
            case Some(item) =>
              val updatedStock = stock.updated(item.itemId, stock(item.itemId) - beneficiary.size)
              ((beneficiary, item) :: paired, updatedStock)
            case None =>
              (paired, stock) // no more items left

    // combine: name-matched first, then order-matched; also return waiting list
    val allocated = nameMatched.reverse ++ orderMatched.reverse
    (allocated, waiting)

  /**
   * EXPORT: Writes allocated pairs to ExportItem.csv.
   * Each row contains FamilyID, ItemID, Size, ExecuteDate.
   */
  // ai-assisted: #8
  // why: Scaffolded file I/O operations to safely read existing lines and append new CSV rows.
  def exportAllocations(
    allocations: List[(Beneficiary, PantryItem)],
    executeDate: String,
    filePath: String
  ): Unit =
    val header = "Family_ID,Item_ID,Size,Execute_Date"
    // read existing rows (skip header) if the file already has content
    val existingRows =
      try
        val lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filePath))
        if lines.size() > 1 then
          (1 until lines.size()).map(i => lines.get(i)).toList.filter(_.trim.nonEmpty)
        else List.empty[String]
      catch
        case _: Exception => List.empty[String]
    val newRows = allocations.map { case (b, item) =>
      s"${b.familyId},${item.itemId},${b.size},$executeDate"
    }
    val content = (header +: (existingRows ++ newRows)).mkString("\n")
    java.nio.file.Files.writeString(java.nio.file.Paths.get(filePath), content)
