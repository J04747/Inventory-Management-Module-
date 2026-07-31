import scala.util.{Success, Failure}

@main def run(): Unit =

  val inventoryPath    = "src/main/scala/PantryItem.csv"
  val beneficiaryPath  = "src/main/scala/Beneficiary.csv"
  val today            = "2026-07-29"

  // ── Load data from CSV files ──
  val inventoryResult = CsvLoader.loadInventory(inventoryPath)
  val requestsResult  = CsvLoader.loadBeneficiaries(beneficiaryPath)

  (inventoryResult, requestsResult) match
    case (Failure(ex), _) =>
      println(s"ERROR loading inventory CSV: ${ex.getMessage}")

    case (_, Failure(ex)) =>
      println(s"ERROR loading beneficiary CSV: ${ex.getMessage}")

    case (Success(inventory), Success(requests)) =>

      // ── Test 1: isExpired ──
      println("=" * 50)
      println("TEST 1: isExpired checks")
      println("=" * 50)
      inventory.foreach: item =>
        val status = if item.isExpired(today) then "EXPIRED" else "OK"
        println(f"  ${item.itemId}%-10s ${item.name}%-22s -> $status")

      // ── Test 2: Beneficiary.findItem ──
      println()
      println("=" * 50)
      println("TEST 2: Beneficiary.findItem")
      println("=" * 50)
      val firstBeneficiary = requests.head
      val found    = firstBeneficiary.findItem("INV-101", inventory)
      val notFound = firstBeneficiary.findItem("X999", inventory)
      println(s"  ${firstBeneficiary.familyName} looking for INV-101: $found")
      println(s"  ${firstBeneficiary.familyName} looking for X999:    $notFound")

      // ── Test 3: SmartMatcher.allocateFood ──
      println()
      println("=" * 50)
      println(s"TEST 3: SmartMatcher.allocateFood (today = $today)")
      println("=" * 50)
      val (allocations, waitingList) = SmartMatcher.allocateFood(inventory, requests, today)

      println(s"  Eligible & Allocated: ${allocations.size}")
      allocations.foreach: (beneficiary, item) =>
        println(f"  ${beneficiary.familyName}%-18s -> ${item.name}%-22s (${item.itemId})  [date: ${beneficiary.dateRequested}]")

      println()
      println(s"  Waiting List: ${waitingList.size}")
      waitingList.foreach: beneficiary =>
        println(f"  ${beneficiary.familyName}%-18s requesting ${beneficiary.requestedFood}%-22s [date: ${beneficiary.dateRequested}]")

      println()
      println("All tests completed.")
