/**
 * REDOPS MOBILE — EXPLICIT DEVELOPMENT & DEMO SEED CLI
 * 
 * Usage: node src/database/seed.js
 * Or:    npm run seed
 * 
 * This script is an explicitly identified development/demo seed mechanism.
 * It is never run as the default production dataset.
 */

const { initDatabase, seedDemoData } = require('./db');

console.log('================================================================');
console.log('  REDOPS MOBILE — EXPLICIT EVALUATION DATASET SEEDER');
console.log('================================================================');

try {
  initDatabase();
  seedDemoData(true);
  console.log('[Seed] Evaluation dataset successfully populated.');
  console.log('[Seed] Assessment: Acme Corp Annual Security Audit');
  console.log('[Seed] User: sh@dow_sec (Password: Password123!Safe)');
  console.log('================================================================');
  process.exit(0);
} catch (err) {
  console.error('[Seed Error] Failed to populate seed data:', err);
  process.exit(1);
}

