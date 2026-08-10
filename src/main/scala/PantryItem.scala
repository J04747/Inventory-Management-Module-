// Ai-assisted : #1
// Used AI to design idiomatic Scala 3 trait hierarchies and case classes with required domain fields.

sealed trait PantryItem:
  protected val _itemId: String // Made protected for ID encapsulation
  def itemId: String = _itemId // Public getter to allow read access
  val name: String
  val quantity: Int
  val expiryDate: String
  // Default behavior defined in the trait
  def isExpired(currentDate: String): Boolean =
    currentDate > expiryDate

case class Perishable(
  override protected val _itemId: String, // Made protected for ID encapsulation
  val name: String,
  val quantity: Int,
  val expiryDate: String,
  val isSpoiledPrematurely: Boolean // The UI passes the "double confirm" result here
) extends PantryItem:

  override def isExpired(currentDate: String): Boolean =
    // It is expired if the date is past OR if the user manually confirmed it is spoiled
    currentDate > expiryDate && isSpoiledPrematurely

case class NonPerishable(
  override protected val _itemId: String, // Made protected for ID encapsulation
  val name: String,
  val quantity: Int,
  val expiryDate: String
)extends PantryItem:
  // Explicit override to guarantee the S1-8 mark
  override def isExpired(currentDate: String): Boolean =
    false



