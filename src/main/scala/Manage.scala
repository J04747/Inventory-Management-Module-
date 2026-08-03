// ai-assisted: #2
// why: Implemented Type Class pattern instances for generic ID extraction.
trait Identifiable[T]:
  def id(item: T): String

object Identifiable:
  given Identifiable[PantryItem] with
    def id(item: PantryItem): String = item.itemId

  given Identifiable[Beneficiary] with
    def id(item: Beneficiary): String = item.familyId

object Manage:
  // ai-assisted: #3
  // why: Generated purely functional CRUD list operations avoiding mutable variables.
  /**
   * CREATE: Adds a new item to the list.
   * Returns a new list (immutable) containing the existing items and the new item.
   */
  def create[T](items: List[T], newItem: T): List[T] =
    items :+ newItem

  /**
   * READ: Finds an item by its ID.
   * Requires a given Identifiable[T] in scope to extract the ID.
   */
  def read[T](items: List[T], targetId: String)(using ev: Identifiable[T]): Option[T] =
    items.find(item => ev.id(item) == targetId)

  /**
   * UPDATE: Replaces an item with a matching ID with the updated item.
   * Returns a new list (immutable) with the item updated.
   */
  def update[T](items: List[T], targetId: String, updatedItem: T)(using ev: Identifiable[T]): List[T] =
    items.map: item =>
      if ev.id(item) == targetId then updatedItem else item

  /**
   * DELETE: Removes an item with a matching ID.
   * Returns a new list (immutable) without the deleted item.
   */
  def delete[T](items: List[T], targetId: String)(using ev: Identifiable[T]): List[T] =
    items.filterNot(item => ev.id(item) == targetId)

  /**
   * SAVE: Writes the current PantryItem list back to the CSV file.
   * Preserves the CSV header row format.
   */
  // ai-assisted: #5
  // why: Provided CSV serialization logic with string interpolation and pattern matching.
  def savePantryItems(items: List[PantryItem], filePath: String): Unit =
    val header = "Type,Item_ID,Item_Name,Stock_Quantity,Expiration_Date"
    val rows = items.map:
      case p: Perishable    => s"Perishable,${p.itemId},${p.name},${p.quantity},${p.expiryDate}"
      case np: NonPerishable => s"NonPerishable,${np.itemId},${np.name},${np.quantity},${np.expiryDate}"
    val content = (header +: rows).mkString("\n")
    java.nio.file.Files.writeString(java.nio.file.Paths.get(filePath), content)

  /**
   * SAVE: Writes the current Beneficiary list back to the CSV file.
   * Preserves the CSV header row format.
   */
  def saveBeneficiaries(items: List[Beneficiary], filePath: String): Unit =
    val header = "Family_ID,Family_Name,Size,Requested_Food,Date_Requested"
    val rows = items.map(b => s"${b.familyId},${b.familyName},${b.size},${b.requestedFood},${b.dateRequested}")
    val content = (header +: rows).mkString("\n")
    java.nio.file.Files.writeString(java.nio.file.Paths.get(filePath), content)

