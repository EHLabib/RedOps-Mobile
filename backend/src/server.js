const path = require('node:path');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const apiRouter = require('./routes/api');

const { CORS_ORIGINS, isProd } = require('./config/env');

const app = express();
const PORT = process.env.PORT || 5000;

// Security Ingress Middleware
app.use(helmet({
  contentSecurityPolicy: false // Allow loading fonts, inline scripts, and local static assets in simulator
}));

// CORS Configuration: Enforce origin allowlist; forbid wildcards in production
const corsOptions = {
  origin: (origin, callback) => {
    // Allow requests without Origin header (mobile apps, native HTTP clients, curl)
    if (!origin) return callback(null, true);

    if (isProd) {
      if (CORS_ORIGINS.includes(origin)) {
        return callback(null, true);
      }
      return callback(new Error(`CORS blocked: Origin "${origin}" is not allowed by policy.`));
    }

    if (CORS_ORIGINS.includes('*') || CORS_ORIGINS.includes(origin)) {
      return callback(null, true);
    }
    return callback(new Error(`CORS blocked: Origin "${origin}" is not allowed by policy.`));
  },
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Target-Authorized', 'X-Security-PIN']
};
app.use(cors(corsOptions));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve Public Web Simulator & Mobile App Testing UI
const publicDir = path.join(__dirname, '..', 'public');
app.use(express.static(publicDir));

// Mount REST API routes under /api/v1
app.use('/api/v1', apiRouter);

// Fallback to Web App for SPA navigation
app.get('*', (req, res, next) => {
  if (req.path.startsWith('/api')) {
    return next();
  }
  res.sendFile(path.join(publicDir, 'index.html'));
});

// Centralized Error Handling Middleware
app.use((err, req, res, next) => {
  console.error('[ServerError]', err.stack || err.message);
  res.status(err.status || 500).json({
    status: 'error',
    message: err.message || 'Internal Server Error'
  });
});

app.listen(PORT, () => {
  console.log('================================================================');
  console.log('  REDOPS MOBILE — AUTHORIZED SECURITY ASSESSMENT PLATFORM');
  console.log('================================================================');
  console.log(`  REST API Server running at: http://localhost:${PORT}/api/v1`);
  console.log(`  Web Testing Simulator running at: http://localhost:${PORT}`);
  console.log('  Notice: Authorized use only.');
  console.log('================================================================');
});
