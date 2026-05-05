'use strict';

// ...existing code...
const express = require("express");
// ...existing code...
const mysql = require("mysql");
const helmet = require("helmet");

// Use environment variables for DB credentials (never hard-code secrets)
const pool = mysql.createPool({
    connectionLimit: 5,
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    charset: 'utf8mb4',
    timeout: 10000
});

const app = express();
app.disable('x-powered-by');

// Security headers
app.use(helmet({
    contentSecurityPolicy: { useDefaults: true },
    crossOriginEmbedderPolicy: false
}));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  res.setHeader('Permissions-Policy', 'geolocation=(), microphone=()');
  next();
});

// Simple in-memory rate limiter (basic protection)
const RATE_LIMIT_MAX = 30;
const RATE_LIMIT_WINDOW_MS = 60_000;
const rateMap = new Map();
app.use((req, res, next) => {
    const ip = req.ip || 'unknown';
    const now = Date.now();
    const entry = rateMap.get(ip) || { count: 0, ts: now };
    if (now - entry.ts > RATE_LIMIT_WINDOW_MS) {
        entry.count = 0;
        entry.ts = now;
    }
    entry.count++;
    rateMap.set(ip, entry);
    if (entry.count > RATE_LIMIT_MAX) {
        return res.status(429).json({ status: 'error', message: 'Too many requests' });
    }
    next();
});

/**
 * Unsubscribe endpoint (GET with email param). For strong security semantics,
 * a POST would be preferable, but implemented per requirement.
 * Does not reveal whether the email exists (prevents enumeration).
 */
app.get("/unsubscribe", (req, res) => {
    // Input extraction & normalization
    let email = req.query.email;
    if (typeof email !== 'string') {
        return safeGenericResponse(res);
    }
    email = email.trim();

    // Length & character validation (prevent abuse / injection primitives even before prepared stmt)
    if (email.length === 0 || email.length > 254) {
        return safeGenericResponse(res);
    }

    // Basic RFC 5322-lite email pattern (strict enough for validation here)
    const emailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;
    if (!emailRegex.test(email)) {
        return safeGenericResponse(res);
    }

    // Use prepared statements (parameterized) to prevent SQL injection (CWE-89)
    // Query then update (avoid timing side-channel enumeration by making responses uniform)
    pool.query(
        'SELECT id, unsubscribed_at FROM subscriptions WHERE email = ? LIMIT 1',
        [email],
        (selErr, results) => {
            if (selErr) {
                // Log minimal error server-side only (avoid leaking internals)
                return safeGenericResponse(res);
            }

            if (!results || results.length === 0) {
                // Email not found; respond generically
                return safeGenericResponse(res);
            }

            const row = results[0];
            // If already unsubscribed, still respond generically
            if (row.unsubscribed_at) {
                return safeGenericResponse(res);
            }

            pool.query(
                'UPDATE subscriptions SET unsubscribed_at = NOW() WHERE id = ?',
                [row.id],
                (updErr) => {
                    if (updErr) {
                        return safeGenericResponse(res);
                    }
                    return safeGenericResponse(res);
                }
            );
        }
    );
});

/**
 * Generic response (no enumeration; no reflection of user input to prevent XSS)
 */
function safeGenericResponse(res) {
    // Ensure JSON only; no user-provided content in output
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    return res.status(200).json({
        status: 'ok',
        message: 'If the address is subscribed it will be (or already is) unsubscribed.'
    });
}

// Global error handler (catch unexpected issues)
app.use((err, req, res, next) => {
    res.status(500).json({ status: 'error', message: 'Internal error' });
});

// Graceful shutdown (release pool)
process.on('SIGINT', () => {
    pool.end(() => {
        process.exit(0);
    });
});

const PORT = process.env.PORT || 3000;
// ...existing code...
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
