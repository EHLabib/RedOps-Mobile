# RedOps Mobile — Backend REST API Module

## Architecture Blueprint
- **Runtime**: Node.js (v18+ LTS)
- **Framework**: Express.js
- **Database**: PostgreSQL 15 (with SQLite local fallback)
- **Authentication**: Stateless JWT + `bcryptjs`
- **Security Middleware**: Helmet, CORS, Express Rate Limit, Input Sanitizers

## Source Tree Structure (To be implemented in Phase 10)
```
backend/
├── src/
│   ├── config/         # Environment variables & constants
│   ├── controllers/    # Request handlers (Auth, Assessments, Findings, etc.)
│   ├── middleware/     # JWT Auth, Role RBAC, Rate Limiter, Error Handler
│   ├── models/         # Database models & SQL queries
│   ├── routes/         # Express API routers
│   ├── services/       # Core business logic & report builders
│   ├── database/       # Connection pool & migration runners
│   ├── utils/          # Token helpers, Logger, CVSS helpers
│   └── server.js       # Express server bootstrapper
├── package.json
└── Dockerfile
```
