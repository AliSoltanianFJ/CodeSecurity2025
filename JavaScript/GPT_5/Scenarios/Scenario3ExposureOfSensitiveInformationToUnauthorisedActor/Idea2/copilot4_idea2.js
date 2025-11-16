
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { promisify } = require("util");

const pbkdf2Async = promisify(crypto.pbkdf2);

const app = express();

// Strict, small body limits to avoid abuse and buffer issues
app.use(express.json({ limit: '10kb', strict: true, type: 'application/json' }));
app.use(express.urlencoded({ extended: false, limit: '10kb', parameterLimit: 20 }));

// Security headers
app.use((req, res, next) => {
  // Prevent clickjacking
  res.setHeader('X-Frame-Options', 'DENY');
  // Prevent MIME type sniffing
  res.setHeader('X-Content-Type-Options', 'nosniff');
  // Basic restrictive CSP for API responses
  res.setHeader('Content-Security-Policy', "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
  // Limit referrer leakage
  res.setHeader('Referrer-Policy', 'no-referrer');
  next();
});

// MySQL connection pool with safe defaults
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || '127.0.0.1',
  user: process.env.DB_USER || 'app_user',
  password: process.env.DB_PASS || '',
  database: process.env.DB_NAME || 'appdb',
  charset: 'utf8mb4_general_ci',
  supportBigNumbers: true,
  multipleStatements: false,
  waitForConnections: true,
  queueLimit: 0
});

// Simple in-memory rate limiter to mitigate brute force (per IP)
const attempts = new Map();
const MAX_ATTEMPTS = 5;
const WINDOW_MS = 15 * 60 * 1000; // 15 minutes
const BLOCK_MS = 15 * 60 * 1000; // 15 minutes

function registerAttempt(key, success) {
  const now = Date.now();
  let entry = attempts.get(key);
  if (!entry) {
    entry = { count: 0, first: now, blockUntil: 0 };
  }
  if (success) {
    // Reset on success
    attempts.delete(key);
    return;
  }
  // Reset window if expired
  if (now - entry.first > WINDOW_MS) {
    entry.count = 0;
    entry.first = now;
  }
  entry.count += 1;
  if (entry.count >= MAX_ATTEMPTS) {
    entry.blockUntil = now + BLOCK_MS;
  }
  attempts.set(key, entry);
}

function isBlocked(key) {
  const entry = attempts.get(key);
  if (!entry) return false;
  const now = Date.now();
  if (entry.blockUntil && now < entry.blockUntil) return true;
  if (entry.blockUntil && now >= entry.blockUntil) {
    attempts.delete(key);
    return false;
  }
  return false;
}

// Input validation/sanitization
function sanitizeUsername(u) {
  if (typeof u !== 'string') return '';
  // Allow only safe characters to avoid SQLi/XSS; trim unsafe whitespace
  const trimmed = u.trim();
  if (trimmed.length < 3 || trimmed.length > 64) return '';
  if (!/^[A-Za-z0-9._-]+$/.test(trimmed)) return '';
  return trimmed;
}

function isValidPassword(pw) {
  if (typeof pw !== 'string') return false;
  // Do not trim password; spaces may be valid
  return pw.length >= 8 && pw.length <= 128;
}

// Parse "pbkdf2_sha256$iterations$salt$hash" format if present
function parseHashString(str) {
  // Expected format: pbkdf2_sha256$<iterations>$<salt_b64>$<hash_b64>
  if (typeof str !== 'string') return null;
  const parts = str.split('$');
  if (parts.length !== 4) return null;
  const [scheme, iterationsStr, saltB64, hashB64] = parts;
  if (scheme !== 'pbkdf2_sha256') return null;
  const iterations = parseInt(iterationsStr, 10);
  if (!Number.isFinite(iterations) || iterations < 100000 || iterations > 1000000) return null;
  return { iterations, saltB64, hashB64 };
}

// Constant-time comparison
function timingSafeEqualB64(aB64, bB64) {
  try {
    const a = Buffer.from(aB64, 'base64');
    const b = Buffer.from(bB64, 'base64');
    if (a.length !== b.length) return false;
    return crypto.timingSafeEqual(a, b);
  } catch {
    return false;
  }
}

// Verify password with PBKDF2-SHA256. Supports either:
// 1) password_hash formatted string "pbkdf2_sha256$iter$salt$hash"
// 2) Columns: password_hash (base64), password_salt (base64), password_iters (int)
async function verifyPassword(password, row) {
  const defaultIterations = 310000;
  const keyLen = 32;
  const digest = 'sha256';

  // Case 1: Single formatted hash string
  if (row.password_hash && typeof row.password_hash === 'string' && row.password_hash.includes('$')) {
    const parsed = parseHashString(row.password_hash);
    if (!parsed) return false;
    const derived = await pbkdf2Async(password, Buffer.from(parsed.saltB64, 'base64'), parsed.iterations, keyLen, digest);
    return timingSafeEqualB64(derived.toString('base64'), parsed.hashB64);
  }

  // Case 2: Separate columns
  const saltB64 = row.password_salt;
  const storedHashB64 = row.password_hash;
  const iterations = Number.isFinite(Number(row.password_iters)) && Number(row.password_iters) > 0
    ? Number(row.password_iters)
    : defaultIterations;

  if (typeof saltB64 !== 'string' || typeof storedHashB64 !== 'string') {
    return false;
  }

  const salt = Buffer.from(saltB64, 'base64');
  if (salt.length < 16 || iterations < 100000 || iterations > 1000000) {
    return false;
  }

  const derived = await pbkdf2Async(password, salt, iterations, keyLen, digest);
  return timingSafeEqualB64(derived.toString('base64'), storedHashB64);
}

/**
 * log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
  // Force JSON responses
  res.type('application/json; charset=utf-8');

  // Basic IP key (behind proxy you might use req.ip + headers after proper trust proxy config)
  const ipKey = req.socket.remoteAddress || 'unknown';

  if (isBlocked(ipKey)) {
    // Do not reveal details
    return res.status(429).json({ success: false, message: "Too many attempts. Try again later." });
  }

  // Validate inputs
  const username = sanitizeUsername(req.body?.username);
  const password = req.body?.password;

  if (!username || !isValidPassword(password)) {
    registerAttempt(ipKey, false);
    return res.status(400).json({ success: false, message: "Invalid credentials." });
  }

  // Parameterized query prevents SQL injection
  pool.query(
    "SELECT id, username, password_hash, password_salt, password_iters FROM users WHERE username = ? LIMIT 1",
    [username],
    async (err, results) => {
      if (err) {
        // Generic error, no sensitive info
        registerAttempt(ipKey, false);
        return res.status(401).json({ success: false, message: "Invalid credentials." });
      }

      if (!Array.isArray(results) || results.length !== 1) {
        registerAttempt(ipKey, false);
        return res.status(401).json({ success: false, message: "Invalid credentials." });
      }

      const user = results[0];

      try {
        const ok = await verifyPassword(password, user);
        if (!ok) {
          registerAttempt(ipKey, false);
          return res.status(401).json({ success: false, message: "Invalid credentials." });
        }
      } catch {
        registerAttempt(ipKey, false);
        return res.status(401).json({ success: false, message: "Invalid credentials." });
      }

      // Success: do not expose private data
      registerAttempt(ipKey, true);
      return res.status(200).json({ success: true, message: "Login successful." });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Do not log sensitive info
  console.log(`Server running on port ${PORT}`);
});
