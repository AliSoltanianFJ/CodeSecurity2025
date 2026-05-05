"use strict";

require("dotenv").config();
const express = require("express");
const mysql = require("mysql");
const helmet = require("helmet");
const argon2 = require("argon2");
const crypto = require("crypto");

const app = express();

// Basic in-memory rate limiter (per IP)
const rateMap = new Map();
const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX = 30;

function rateLimit(req, res, next) {
  const ip = req.ip || "unknown";
  const now = Date.now();
  const entry = rateMap.get(ip) || { count: 0, start: now };
  if (now - entry.start > RATE_LIMIT_WINDOW_MS) {
    entry.count = 0;
    entry.start = now;
  }
  entry.count += 1;
  rateMap.set(ip, entry);
  if (entry.count > RATE_LIMIT_MAX) {
    return res.status(429).json({ status: "error", message: "Too many requests" });
  }
  next();
}

// Security headers
app.use(helmet({
  frameguard: { action: "deny" },
  xssFilter: true,
  noSniff: true,
  hidePoweredBy: true,
}));
app.use((req, res, next) => {
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
  next();
});

// Body parsers with size limits
app.use(express.json({ limit: "10kb", strict: true }));
app.use(express.urlencoded({ extended: false, limit: "10kb", parameterLimit: 10 }));

// MySQL connection pool (credentials via environment variables)
const db = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 5,
  charset: "utf8mb4",
  multipleStatements: false
});

// Simple HTML escape (avoid reflecting unsanitized input)
function escapeHtml(str) {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// Validate and sanitize username
function validateUsername(raw) {
  if (typeof raw !== "string") return null;
  const trimmed = raw.trim();
  if (trimmed.length < 3 || trimmed.length > 32) return null;
  if (!/^[A-Za-z0-9_]+$/.test(trimmed)) return null;
  return trimmed;
}

// Validate password complexity
function validatePassword(raw) {
  if (typeof raw !== "string") return null;
  if (raw.length < 12 || raw.length > 128) return null;
  if (!/[a-z]/.test(raw)) return null;
  if (!/[A-Z]/.test(raw)) return null;
  if (!/[0-9]/.test(raw)) return null;
  if (!/[!@#$%^&*()[\]{}_<>,.?;:~\-+=]/.test(raw)) return null;
  if (/\s{2,}/.test(raw)) return null;
  return raw;
}

// Avoid insecure deserialization: reject non-primitive unexpected types
function ensurePrimitives(body) {
  if (body == null || typeof body !== "object") return false;
  for (const k of Object.keys(body)) {
    const v = body[k];
    if (typeof v === "object") return false;
  }
  return true;
}

/**
 * POST /register
 * Secure user registration
 */
app.post("/register", rateLimit, async (req, res) => {
  try {
    if (!ensurePrimitives(req.body)) {
      return res.status(400).json({ status: "error", message: "Invalid payload" });
    }

    const username = validateUsername(req.body.username);
    const password = validatePassword(req.body.password);

    if (!username || !password) {
      return res.status(400).json({ status: "error", message: "Invalid credentials" });
    }

    // Generate strong password hash (argon2id)
    const hash = await argon2.hash(password, {
      type: argon2.argon2id,
      timeCost: 3,
      memoryCost: 19456,
      parallelism: 1
    });

    // Insert using parameterized query
    db.query(
      "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())",
      [username, hash],
      (err) => {
        if (err) {
          if (err.code === "ER_DUP_ENTRY") {
            return res.status(409).json({ status: "error", message: "Username taken" });
          }
          return res.status(500).json({ status: "error", message: "Registration failed" });
        }
        // Do not expose sensitive info
        return res.status(201).json({
          status: "ok",
          message: "User registered",
          user: { username: escapeHtml(username) }
        });
      }
    );
  } catch (e) {
    return res.status(500).json({ status: "error", message: "Server error" });
  }
});

// Generic health endpoint
app.get("/health", (req, res) => {
  res.json({ status: "ok" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // No sensitive info in logs
  console.log(`Server running on port ${PORT}`);
});
