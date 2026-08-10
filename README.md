# Cali's Bloomprints POS

Offline-first Android point-of-sale app for a flower and keepsake shop. The project is built with Kotlin, Jetpack Compose, Room, and Bluetooth ESC/POS printing so shop staff can sell, track stock, and print receipts even without an internet connection.

## Portfolio Highlights

- Offline-first Room database for products, sales, sale items, stock movements, and printer settings.
- Product catalog seeded with flower shop items such as bouquets, single stems, gifts, prints, and custom arrangements.
- Touch-friendly cart with quantity controls, stock validation, customer name, customer contact, and order notes.
- Cash checkout with amount tendered, change calculation, optional pickup details, and saved receipt records.
- Sales history with line-item details and receipt reprint support.
- Inventory screen with manual stock adjustments and tracked/untracked product handling.
- Bluetooth paired-printer selection using Android Bluetooth Classic SPP and ESC/POS commands.
- Saved printer device, saved paper width for 58 mm or 80 mm receipts, test print, and receipt print.
- Receipt branding options for shop name, subtitle, footer text, optional logo, and auto-print behavior.
- Early report/export work for daily sales summaries plus PDF/XLSX report output.

## Current App Areas

- **Sell:** browse products by category, add items to the cart, enter customer/order details, calculate totals, and complete cash checkout.
- **Sales:** review recent transactions, inspect items, reprint receipts, and track pickup orders.
- **Inventory:** edit catalog items, adjust stock, watch low-stock products, and review stock movement history.
- **Printer:** refresh paired devices, choose a printer, choose receipt width, save branding, run a test print, and print receipts.
- **Reports:** generate sales summaries and export report files from the Android share/save flow.

## Tech Stack

- Kotlin
- Jetpack Compose and Material 3
- Room database with migrations
- Kotlin coroutines and Flow
- Android Bluetooth Classic printing
- ESC/POS receipt command generation
- Gradle Kotlin DSL

## Roadmap

### Phase 1: Core Offline POS

- Product catalog, cart, customer info, notes, cash checkout, change calculation, sales history, stock adjustment, and receipt reprint.
- Bluetooth paired-printer selection, saved paper width, test print, and receipt print.

### Phase 2: Checkout And Shop Operations

- Discounts, service fees, deposits, and partial payments.
- Daily sales summary with cash drawer totals.
- Low-stock alerts and stock movement history.
- Receipt branding and footer settings.

### Phase 3: Reliability And Reports

- CSV export for sales and product data.
- Backup and restore for the local Room database.
- Refunds, voids, and manager PIN approval.
- Z-reading style daily close.
- Printer diagnostics for code page, line width, paper cut, drawer kick, and connection checks.

### Phase 4: Growth

- Optional cloud sync across devices.
- Pickup reminders and customer order calendar improvements.
- Delivery tracking, customer history, and staff roles.

## Open In Android Studio

1. Open this repository folder in Android Studio.
2. Let Gradle sync and install any missing Android SDK components.
3. Pair a Bluetooth ESC/POS thermal printer in Android system settings.
4. Run the app on a physical Android device.
5. Open `Printer`, grant the Nearby Devices permission, refresh paired printers, select a printer, choose a paper width, and run `Test Print`.

## Privacy Notes

This portfolio copy is prepared without local Android Studio folders, APK/AAB outputs, signing keys, device-specific settings, real databases, exported reports, or local machine configuration. The starter catalog uses demo product data only.

## Screenshots

Add portfolio screenshots to `docs/screenshots/` before publishing the repository.
