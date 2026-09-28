# Food Pantry Management System

## Overview
The Food Pantry Management System is a **Scala 3** application with a **ScalaFX** graphical user interface designed to help a food pantry easily track inventory, record beneficiary requests, and automatically allocate food to those in need. It features a robust "Smart Match" algorithm to dynamically optimize allocations.

## Features
- **Inventory Management (CRUD)**: Manage pantry items which can be either `Perishable` or `NonPerishable`. Tracks essential details like quantities and expiry dates.
- **Beneficiary Management (CRUD)**: Maintain records of families, their sizes, what food they requested, and the date the request was made.
- **Smart Match Algorithm**: Automatically pairs eligible beneficiaries with available inventory using pure Functional Programming (FP). It factors in requested dates, specific food preferences, and remaining stock quantites.
- **Manual Verification Check**: Requires manual confirmation for perishable goods to guarantee they are not spoiled before the system finalizes their allocation.
- **Data Persistence**: Loads and saves all data persistently via localized CSV files (`PantryItem.csv`, `Beneficiary.csv`, `ExportItem.csv`).

## Architecture & Code Structure

The source code is structured as follows:

*   **`MainApp.scala`**: The main entry point. Houses the ScalaFX GUI layout, routing, and screens (View, Manage, Smart Match, Alerts/Export).
*   **`PantryItem.scala`**: The domain model for the inventory, featuring a sealed trait hierarchy that encapsulates IDs and differentiates between perishable and non-perishable goods.
*   **`Beneficiary.scala`**: The domain model for beneficiaries tracking family sizes and requests.
*   **`SmartMatcher.scala`**: The core allocation logic. Utilizes pure FP patterns (`partition`, `foldLeft`) to manage state without mutability while assigning items in two passes (name-matched, then order-matched).
*   **`Manage.scala`**: A service module providing generic CRUD utilities to handle updates on the immutable data lists and save changes to disk.
*   **`CsvLoader.scala`**: Handles safe parsing and loading of the initial state from the CSV resource files.

## AI Assistance Context

This codebase incorporates some AI-assisted blocks, labeled explicitly with `// ai-assisted: #N` tags along with `// why:` comments detailing the reasoning. Examples include:
*   Designing idiomatic Scala 3 trait hierarchies.
*   Scaffolding repetitive ScalaFX component logic (e.g., dynamic forms and `TableCell` factories).
*   Orchestrating complex immutable state transformations using `foldLeft`.
*   Scaffolding safe File I/O operations.
## Third-Party Code & Assets

This project utilizes the following third-party open-source libraries:
- **[scala-csv](https://github.com/tototoshi/scala-csv)**: Used for robust parsing and loading of CSV files. Licensed under the **Apache License 2.0**.
- **[ScalaFX](https://github.com/scalafx/scalafx)**: Used for the graphical user interface components. Licensed under the **BSD 3-Clause License**.

## Running the Application

To run the application, ensure you have Java and sbt installed. From the project root directory, run:

```bash
sbt run
```