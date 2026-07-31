

class Beneficiary(
  val familyId: String,
  val familyName: String,
  val size: Int,
  val requestedFood: String,
  val dateRequested: String
):
  def findItem(itemId: String, pantry: List[PantryItem]): Option[PantryItem] =
    pantry.find(_.itemId == itemId)
