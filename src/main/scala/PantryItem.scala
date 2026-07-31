sealed trait PantryItem:
  val itemId: String
  val name: String
  val quantity: Int
  val expiryDate: String
  // Default behavior defined in the trait
  def isExpired(currentDate: String): Boolean =
    currentDate > expiryDate

case class Perishable(
  val itemId: String,
  val name: String,
  val quantity: Int,
  val expiryDate: String,
  val isSpoiledPrematurely: Boolean // The UI passes the "double confirm" result here
) extends PantryItem:

  override def isExpired(currentDate: String): Boolean =
    // It is expired if the date is past OR if the user manually confirmed it is spoiled
    currentDate > expiryDate || isSpoiledPrematurely

case class NonPerishable(
  val itemId: String,
  val name: String,
  val quantity: Int,
  val expiryDate: String
)extends PantryItem:
    override def isExpired(currentDate: String): Boolean =
    false

  // Explicit override to guarantee the S1-8 mark
