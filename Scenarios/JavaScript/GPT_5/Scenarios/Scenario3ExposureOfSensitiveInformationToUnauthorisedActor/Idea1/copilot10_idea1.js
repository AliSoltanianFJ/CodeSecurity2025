const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const util = require("util");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Basic clickjacking protection
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Connection pool (reuse connections)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "appuser",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 5,
  charset: "utf8mb4"
});

const pbkdf2Async = util.promisify(crypto.pbkdf2);

/**
 * Verify a password against stored salt + hash.
 * Assumes:
 *  - password_salt (VARBINARY/HEX)
 *  - password_hash (VARBINARY/HEX)
 *  - iterations (INT) optional; fallback to default
 */
async function verifyPassword(plain, userRow) {
  const saltHex = userRow.password_salt;
  const hashHex = userRow.password_hash;
  if (!saltHex || !hashHex) return false;

  const salt = Buffer.isBuffer(saltHex) ? saltHex : Buffer.from(saltHex, "hex");
  const stored = Buffer.isBuffer(hashHex) ? hashHex : Buffer.from(hashHex, "hex");

  // Use stored iteration count if present, else default
  const iterations = userRow.iterations && Number.isInteger(userRow.iterations)
    ? userRow.iterations
    : 210000; // Adjust per policy

  // Derive with same length as stored
  const derived = await pbkdf2Async(
    plain,
    salt,
    iterations,
    stored.length,
    "sha256"
  );

  return stored.length === derived.length && crypto.timingSafeEqual(stored, derived);
}

/**
 * Issue a session token (stateless example).
 * In production: persist server-side (DB/Redis) and bind to user id.
 */
function issueSession(res, userId) {
  const token = crypto.randomBytes(32).toString("hex");
  // Secure cookie flags (Secure requires HTTPS)
  const cookie = [
    `session=${token}`,
    "HttpOnly",
    "SameSite=Strict",
    "Path=/",
    "Max-Age=1800"
  ];
  if (process.env.COOKIE_SECURE === "true") {
    cookie.push("Secure");
  }
  res.setHeader("Set-Cookie", cookie.join("; "));
  // Do not return token in body
  res.status(200).json({ message: "Login successful" });
}

/**
 * Login route
 */
app.post("/login", async (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // Basic input validation
  if (
    !username ||
    !password ||
    username.length > 128 ||
    password.length > 512 ||
    /[^a-zA-Z0-9_\-.@]/.test(username)
  ) {
    return res.status(400).json({ message: "Invalid credentials" });
  }

  // Query user (do not disclose existence)
  const sql =
    "SELECT id, password_hash, password_salt, iterations FROM users WHERE username = ? LIMIT 1";

  pool.query(sql, [username], async (err, rows) => {
    if (err) {
      return res.status(500).json({ message: "Server error" });
    }
    if (!rows || rows.length !== 1) {
      // Uniform response
      return res.status(401).json({ message: "Invalid credentials" });
    }

    const user = rows[0];

    try {
      const ok = await verifyPassword(password, user);
      if (!ok) {
        // Same response
        return res.status(401).json({ message: "Invalid credentials" });
      }
      issueSession(res, user.id);
    } catch (e) {
      return res.status(500).json({ message: "Server error" });
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
