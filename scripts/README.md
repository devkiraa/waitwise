# WaitWise — Menu Seeding Tool & Documentation

This directory contains the Node.js menu seeding tool (`seed-menu.js`) used to populate 32 realistic Indian canteen food items into the **WaitWise** Firebase Realtime Database.

---

## 1. Prerequisites

- **Node.js**: v18.0.0 or higher
- **Firebase Service Account Key**: Downloaded from the Firebase Console (`waitwise-in` project).
- **Firebase Database URL**: `https://waitwise-in-default-rtdb.asia-southeast1.firebasedatabase.app`

---

## 2. Installation

Open **Windows PowerShell** in the `scripts/` folder and install the required dependencies:

```powershell
cd C:\Users\kiran\StudioProjects\waitwise\scripts
npm install
```

---

## 3. How to Get Your Firebase Service Account JSON File

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Select your project: **WaitWise (`waitwise-in`)**.
3. Click the gear icon ⚙️ next to *Project Overview* -> **Project Settings**.
4. Select the **Service accounts** tab.
5. Click **Generate new private key** and save the `.json` file to a secure directory on your computer (e.g. `C:\Users\kiran\serviceAccountKey.json`).
6. **Important Security Note**:
   - The Service Account key grants administrative access to your Firebase Realtime Database.
   - **Never** place the service account key inside the `app/` folder or commit it to Git.
   - It is automatically ignored in `.gitignore`.

---

## 4. Running a Dry-Run (Preview Mode)

To preview the 32 canteen food items without connecting or modifying Firebase:

```powershell
node seed-menu.js --dry-run
```

Output summary:
```text
[DRY-RUN MODE] Previewing menu items to be seeded...
Database path target: /menu

[1/32] food_idli_sambar
   Name:        Idli Sambar (2 Pcs)
   Category:    Breakfast
   Price:       ₹35.00
   Description: Steamed fluffy rice cakes served hot with piping lentil sambar and fresh coconut chutney.
...
--- DRY-RUN SUMMARY ---
Total prepared canteen items: 32
  - Breakfast: 8 items
  - Meals: 8 items
  - Snacks: 8 items
  - Drinks: 8 items

Notice: No changes were made to Firebase Realtime Database.
```

---

## 5. Applying Changes to Firebase Realtime Database

Set the environment variables in PowerShell and run with `--apply`:

```powershell
# Set Service Account credentials path
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\Users\kiran\serviceAccountKey.json"

# (Optional) Set Database URL if custom URL is used
$env:FIREBASE_DATABASE_URL="https://waitwise-in-default-rtdb.asia-southeast1.firebasedatabase.app"

# Apply menu items to Firebase Realtime Database
node seed-menu.js --apply
```

Output summary:
```text
[APPLY MODE] Initializing Firebase Admin SDK connection...
Connecting to Firebase Database: https://waitwise-in-default-rtdb.asia-southeast1.firebasedatabase.app
Target database path: /menu

 [ADDED]    food_idli_sambar - Idli Sambar (2 Pcs) (₹35)
 [ADDED]    food_masala_dosa - Crispy Masala Dosa (₹60)
 ...
--- SEEDING COMPLETE ---
Items Added:   32
Items Updated: 0
Errors:        0
Total Target:  32
```

---

## 6. Verification Steps

1. Open the [Firebase Realtime Database Console](https://console.firebase.google.com/u/0/project/waitwise-in/database/waitwise-in-default-rtdb/data).
2. Expand the `/menu` node. Verify that all 32 items exist with deterministic IDs (`food_idli_sambar`, `food_masala_dosa`, etc.).
3. Build and launch the **WaitWise Android App**:
   - Open **Customer Home**: Verify items appear cleanly in the menu list with category filtering (*Breakfast, Meals, Snacks, Drinks*).
   - Open **Admin Dashboard**: Verify total items counter shows `32`.
   - Open **Admin Manage Products**: Verify all 32 products can be searched, edited, or toggled for stock availability.

---

## 7. Safety & Idempotency Guarantees

- **Deterministic IDs**: Running `--apply` multiple times updates existing fields in place rather than creating duplicate items.
- **Data Protection**: Modifies only `/menu`. Does not delete or overwrite `/orders`, `/users`, `/carts`, `/favorites`, `/tokens`, or `/notifications`.
