/**
 * WaitWise — Firebase Realtime Database Menu Seeding Script
 *
 * Seeding script that populates realistic canteen food items under /menu
 * in Firebase Realtime Database for project: waitwise-in
 *
 * Usage:
 *   node seed-menu.js --dry-run
 *   node seed-menu.js --apply
 *
 * Requirements:
 *   FIREBASE_DATABASE_URL (optional, defaults to https://waitwise-in-default-rtdb.asia-southeast1.firebasedatabase.app)
 *   GOOGLE_APPLICATION_CREDENTIALS (path to service account JSON file, required for --apply)
 */

const fs = require('fs');
const path = require('path');

// 32 Realistic Canteen Food Items across Breakfast, Meals, Snacks, Drinks
const SEED_MENU_ITEMS = [
  // --- BREAKFAST ---
  {
    id: 'food_idli_sambar',
    name: 'Idli Sambar (2 Pcs)',
    description: 'Steamed fluffy rice cakes served hot with piping lentil sambar and fresh coconut chutney.',
    price: 35.0,
    category: 'Breakfast',
    imageUrl: 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?q=80&w=800',
    available: true
  },
  {
    id: 'food_masala_dosa',
    name: 'Crispy Masala Dosa',
    description: 'Golden crispy rice crepe stuffed with spiced potato masala, served with sambar & chutneys.',
    price: 60.0,
    category: 'Breakfast',
    imageUrl: 'https://images.unsplash.com/photo-1668236543090-82eba5ee5976?q=80&w=800',
    available: true
  },
  {
    id: 'food_plain_dosa',
    name: 'Crispy Plain Dosa',
    description: 'Classic thin crispy rice crepe served with coconut chutney and tangy sambar.',
    price: 45.0,
    category: 'Breakfast',
    imageUrl: 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?q=80&w=800',
    available: true
  },
  {
    id: 'food_poori_masala',
    name: 'Poori Masala (2 Pcs)',
    description: 'Deep-fried fluffy whole wheat pooris served with flavorful potato curry.',
    price: 50.0,
    category: 'Breakfast',
    imageUrl: 'https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?q=80&w=800',
    available: true
  },
  {
    id: 'food_ven_pongal',
    name: 'Hot Ven Pongal',
    description: 'Traditional rice and moong dal ghee pongal tempered with black pepper, cumin, and cashews.',
    price: 45.0,
    category: 'Breakfast',
    imageUrl: 'https://images.unsplash.com/photo-1601050690597-df0568f70950?q=80&w=800',
    available: true
  },
  {
    id: 'food_medu_vada',
    name: 'Medu Vada (2 Pcs)',
    description: 'Crispy golden fried lentil donuts seasoned with pepper and ginger, served with chutney.',
    price: 35.0,
    category: 'Breakfast',
    imageUrl: 'https://images.unsplash.com/photo-1601050690597-df0568f70950?q=80&w=800',
    available: true
  },
  {
    id: 'food_onion_uttapam',
    name: 'Onion Uttapam',
    description: 'Thick savory rice pancake topped with finely chopped onions, green chilies, and herbs.',
    price: 55.0,
    category: 'Breakfast',
    imageUrl: 'https://images.unsplash.com/photo-1668236543090-82eba5ee5976?q=80&w=800',
    available: true
  },
  {
    id: 'food_aloo_paratha',
    name: 'Aloo Paratha with Curd',
    description: 'Whole wheat flatbread stuffed with spiced potato mash, served with fresh curd and pickle.',
    price: 60.0,
    category: 'Breakfast',
    imageUrl: 'https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?q=80&w=800',
    available: true
  },

  // --- MEALS ---
  {
    id: 'food_veg_meals',
    name: 'South Indian Veg Meals',
    description: 'Steamed rice served with sambar, rasam, kootu, poriyal, curd, papad, and pickle.',
    price: 90.0,
    category: 'Meals',
    imageUrl: 'https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?q=80&w=800',
    available: true
  },
  {
    id: 'food_veg_fried_rice',
    name: 'Indo-Chinese Veg Fried Rice',
    description: 'Wok-tossed basmati rice with finely chopped garden vegetables and aromatic soy sauce.',
    price: 80.0,
    category: 'Meals',
    imageUrl: 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?q=80&w=800',
    available: true
  },
  {
    id: 'food_veg_noodles',
    name: 'Hakka Veg Noodles',
    description: 'Stir-fried noodles tossed with crunchy carrots, cabbage, capsicum, and savory sauces.',
    price: 80.0,
    category: 'Meals',
    imageUrl: 'https://images.unsplash.com/photo-1585032226651-759b368d7246?q=80&w=800',
    available: true
  },
  {
    id: 'food_curd_rice',
    name: 'Comfort Curd Rice',
    description: 'Soothing seasoned curd rice tempered with mustard seeds, curry leaves, and pomegranate.',
    price: 50.0,
    category: 'Meals',
    imageUrl: 'https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?q=80&w=800',
    available: true
  },
  {
    id: 'food_lemon_rice',
    name: 'Tangy Lemon Rice',
    description: 'Fragrant rice tempered with turmeric, fresh lemon juice, roasted peanuts, and green chilies.',
    price: 50.0,
    category: 'Meals',
    imageUrl: 'https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?q=80&w=800',
    available: true
  },
  {
    id: 'food_paneer_chapati',
    name: 'Chapati with Paneer Gravy (3 Pcs)',
    description: 'Soft whole wheat chapatis served with rich & creamy paneer butter masala gravy.',
    price: 95.0,
    category: 'Meals',
    imageUrl: 'https://images.unsplash.com/photo-1631452180519-c014fe946bc7?q=80&w=800',
    available: true
  },
  {
    id: 'food_schezwan_fried_rice',
    name: 'Spicy Schezwan Fried Rice',
    description: 'Fiery wok-tossed fried rice cooked in house Schezwan sauce and spring onions.',
    price: 85.0,
    category: 'Meals',
    imageUrl: 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?q=80&w=800',
    available: true
  },
  {
    id: 'food_gobi_manchurian',
    name: 'Gobi Manchurian Dry',
    description: 'Crispy cauliflower florets tossed in tangy Indo-Chinese garlic-soy glaze with green onions.',
    price: 85.0,
    category: 'Meals',
    imageUrl: 'https://images.unsplash.com/photo-1585032226651-759b368d7246?q=80&w=800',
    available: true
  },

  // --- SNACKS ---
  {
    id: 'food_samosa',
    name: 'Crispy Aloo Samosa (2 Pcs)',
    description: 'Golden flaky pastry stuffed with spiced potato & green peas mash, served with sweet chutney.',
    price: 30.0,
    category: 'Snacks',
    imageUrl: 'https://images.unsplash.com/photo-1601050690597-df0568f70950?q=80&w=800',
    available: true
  },
  {
    id: 'food_veg_puff',
    name: 'Bakery Veg Puff',
    description: 'Flaky multi-layered baked puff pastry filled with spicy mixed vegetable filling.',
    price: 25.0,
    category: 'Snacks',
    imageUrl: 'https://images.unsplash.com/photo-1601050690597-df0568f70950?q=80&w=800',
    available: true
  },
  {
    id: 'food_paneer_roll',
    name: 'Grilled Paneer Roll',
    description: 'Soft kathia paratha wrapped around marinated grilled paneer cubes, onions, and mint sauce.',
    price: 70.0,
    category: 'Snacks',
    imageUrl: 'https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?q=80&w=800',
    available: true
  },
  {
    id: 'food_french_fries',
    name: 'Crispy Salted French Fries',
    description: 'Deep-fried golden potato fries seasoned with salt and served with tomato ketchup.',
    price: 60.0,
    category: 'Snacks',
    imageUrl: 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?q=80&w=800',
    available: true
  },
  {
    id: 'food_veg_burger',
    name: 'Classic Veg Patty Burger',
    description: 'Crispy vegetable patty in a toasted sesame bun with lettuce, tomatoes, and mayonnaise.',
    price: 65.0,
    category: 'Snacks',
    imageUrl: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?q=80&w=800',
    available: true
  },
  {
    id: 'food_cheese_sandwich',
    name: 'Grilled Cheese & Corn Sandwich',
    description: 'Golden toasted bread stuffed with melted cheese, sweet corn kernels, and Italian herbs.',
    price: 55.0,
    category: 'Snacks',
    imageUrl: 'https://images.unsplash.com/photo-1528735602780-2552fd46c7af?q=80&w=800',
    available: true
  },
  {
    id: 'food_veg_sandwich',
    name: 'Bombay Veg Toast Sandwich',
    description: 'Toasted sandwich layered with spiced potato mash, cucumber, tomato, and mint chutney.',
    price: 40.0,
    category: 'Snacks',
    imageUrl: 'https://images.unsplash.com/photo-1528735602780-2552fd46c7af?q=80&w=800',
    available: true
  },
  {
    id: 'food_pav_bhaji',
    name: 'Mumbai Butter Pav Bhaji',
    description: 'Buttered toasted pav buns served with rich spiced vegetable bhaji, lemon wedge, and onions.',
    price: 75.0,
    category: 'Snacks',
    imageUrl: 'https://images.unsplash.com/photo-1601050690597-df0568f70950?q=80&w=800',
    available: true
  },

  // --- DRINKS ---
  {
    id: 'food_masala_tea',
    name: 'Hot Masala Chai',
    description: 'Freshly brewed milk tea infused with crushed cardamom, fresh ginger, and spices.',
    price: 15.0,
    category: 'Drinks',
    imageUrl: 'https://images.unsplash.com/photo-1576092768241-dec231879fc3?q=80&w=800',
    available: true
  },
  {
    id: 'food_filter_coffee',
    name: 'South Indian Filter Coffee',
    description: 'Authentic strong decoction coffee frothed with hot milk in traditional brass style.',
    price: 20.0,
    category: 'Drinks',
    imageUrl: 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?q=80&w=800',
    available: true
  },
  {
    id: 'food_fresh_lime_water',
    name: 'Fresh Lime Water / Soda',
    description: 'Chilled fresh lime juice with sweet syrup and sparkling soda water.',
    price: 25.0,
    category: 'Drinks',
    imageUrl: 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?q=80&w=800',
    available: true
  },
  {
    id: 'food_masala_buttermilk',
    name: 'Spiced Masala Buttermilk',
    description: 'Refreshing chilled churned yogurt drink flavored with green chilies, ginger, and cilantro.',
    price: 20.0,
    category: 'Drinks',
    imageUrl: 'https://images.unsplash.com/photo-1556881286-fc6915169721?q=80&w=800',
    available: true
  },
  {
    id: 'food_sweet_lassi',
    name: 'Creamy Punjabi Sweet Lassi',
    description: 'Rich blended sweet yogurt drink topped with malai and cardamom powder.',
    price: 40.0,
    category: 'Drinks',
    imageUrl: 'https://images.unsplash.com/photo-1556881286-fc6915169721?q=80&w=800',
    available: true
  },
  {
    id: 'food_chocolate_milkshake',
    name: 'Thick Chocolate Milkshake',
    description: 'Cold thick chocolate milkshake blended with chocolate syrup and cocoa powder.',
    price: 60.0,
    category: 'Drinks',
    imageUrl: 'https://images.unsplash.com/photo-1572490122747-3968b75cc699?q=80&w=800',
    available: true
  },
  {
    id: 'food_cold_coffee',
    name: 'Chilled Cold Coffee',
    description: 'Blended iced coffee with milk, sugar, and vanilla ice cream scoop.',
    price: 50.0,
    category: 'Drinks',
    imageUrl: 'https://images.unsplash.com/photo-1517701604599-bb29b565090c?q=80&w=800',
    available: true
  },
  {
    id: 'food_mango_shake',
    name: 'Fresh Mango Milkshake',
    description: 'Delicious seasonal Alphonso mango pulp blended with chilled milk and ice.',
    price: 55.0,
    category: 'Drinks',
    imageUrl: 'https://images.unsplash.com/photo-1546173159-315724a31696?q=80&w=800',
    available: true
  }
];

async function main() {
  const args = process.argv.slice(2);
  const isApply = args.includes('--apply');
  const isDryRun = args.includes('--dry-run') || !isApply;

  console.log('---------------------------------------------------------');
  console.log('          WaitWise — Menu Seeding Tool                   ');
  console.log('---------------------------------------------------------');

  if (isDryRun && !isApply) {
    console.log('[DRY-RUN MODE] Previewing menu items to be seeded...');
    console.log('Database path target: /menu\n');

    let categoryCounts = {};
    SEED_MENU_ITEMS.forEach((item, index) => {
      categoryCounts[item.category] = (categoryCounts[item.category] || 0) + 1;
      console.log(`[${index + 1}/${SEED_MENU_ITEMS.length}] ${item.id}`);
      console.log(`   Name:        ${item.name}`);
      console.log(`   Category:    ${item.category}`);
      console.log(`   Price:       ₹${item.price.toFixed(2)}`);
      console.log(`   Description: ${item.description}`);
      console.log(`   Image URL:   ${item.imageUrl}`);
      console.log('---------------------------------------------------------');
    });

    console.log('\n--- DRY-RUN SUMMARY ---');
    console.log(`Total prepared canteen items: ${SEED_MENU_ITEMS.length}`);
    Object.keys(categoryCounts).forEach(cat => {
      console.log(`  - ${cat}: ${categoryCounts[cat]} items`);
    });
    console.log('\nNotice: No changes were made to Firebase Realtime Database.');
    console.log('To write these items to Firebase, run with the --apply flag:');
    console.log('  node scripts/seed-menu.js --apply\n');
    process.exit(0);
  }

  // Apply Mode
  console.log('[APPLY MODE] Initializing Firebase Admin SDK connection...');

  const dbUrl = process.env.FIREBASE_DATABASE_URL || 'https://waitwise-in-default-rtdb.asia-southeast1.firebasedatabase.app';
  const credPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;

  if (!credPath) {
    console.error('\nERROR: GOOGLE_APPLICATION_CREDENTIALS environment variable is not set!');
    console.error('Please set it to the path of your Firebase Service Account JSON file.');
    console.error('Example (PowerShell):');
    console.error('  $env:GOOGLE_APPLICATION_CREDENTIALS="C:\\path\\to\\serviceAccountKey.json"');
    console.error('  node scripts/seed-menu.js --apply\n');
    process.exit(1);
  }

  if (!fs.existsSync(credPath)) {
    console.error(`\nERROR: Service Account file not found at path: ${credPath}`);
    process.exit(1);
  }

  let admin;
  try {
    admin = require('firebase-admin');
  } catch (e) {
    console.error('\nERROR: firebase-admin npm package is not installed!');
    console.error('Please run: npm install inside the scripts directory.\n');
    process.exit(1);
  }

  try {
    admin.initializeApp({
      credential: admin.credential.applicationDefault(),
      databaseURL: dbUrl
    });
  } catch (err) {
    console.error('Failed to initialize Firebase Admin SDK:', err.message);
    process.exit(1);
  }

  const db = admin.database();
  const menuRef = db.ref('menu');

  console.log(`Connecting to Firebase Database: ${dbUrl}`);
  console.log('Target database path: /menu\n');

  let addedCount = 0;
  let updatedCount = 0;
  let errorCount = 0;

  try {
    const snapshot = await menuRef.once('value');
    const existingMenu = snapshot.val() || {};

    for (const seedItem of SEED_MENU_ITEMS) {
      const itemId = seedItem.id;
      const existingItem = existingMenu[itemId];

      const payload = {
        id: itemId,
        name: seedItem.name,
        description: seedItem.description,
        price: seedItem.price,
        category: seedItem.category,
        imageUrl: seedItem.imageUrl,
        available: seedItem.available,
        createdAt: existingItem && existingItem.createdAt ? existingItem.createdAt : Date.now()
      };

      try {
        await menuRef.child(itemId).update(payload);
        if (existingItem) {
          console.log(` [UPDATED]  ${itemId} - ${seedItem.name} (₹${seedItem.price})`);
          updatedCount++;
        } else {
          console.log(` [ADDED]    ${itemId} - ${seedItem.name} (₹${seedItem.price})`);
          addedCount++;
        }
      } catch (itemErr) {
        console.error(` [ERROR]    Failed to write ${itemId}: ${itemErr.message}`);
        errorCount++;
      }
    }

    console.log('\n---------------------------------------------------------');
    console.log('--- SEEDING COMPLETE ---');
    console.log(`Items Added:   ${addedCount}`);
    console.log(`Items Updated: ${updatedCount}`);
    console.log(`Errors:        ${errorCount}`);
    console.log(`Total Target:  ${SEED_MENU_ITEMS.length}`);
    console.log('---------------------------------------------------------');

    await admin.app().delete();

    if (errorCount > 0) {
      process.exit(1);
    } else {
      process.exit(0);
    }
  } catch (dbErr) {
    console.error('\nDatabase operation failed:', dbErr.message);
    await admin.app().delete();
    process.exit(1);
  }
}

main().catch(err => {
  console.error('Unhandled script error:', err);
  process.exit(1);
});
