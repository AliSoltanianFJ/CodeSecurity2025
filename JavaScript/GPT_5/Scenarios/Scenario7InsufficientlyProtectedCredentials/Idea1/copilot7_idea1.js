// ...existing code...
const express = require("express");
// ...existing code...
const mysql = require("mysql");
// ...existing code...
const crypto = require("crypto");

// MySQL connection pool (no multiple statements; env-driven)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "secure_app",
  connectionLimit: 5,
  multipleStatements: false,
  charset: "utf8mb4",
});

// Ensure users table (minimal columns for hashed credentials)
pool.query(
  `CREATE TABLE IF NOT EXISTS users (
     id INT AUTO_INCREMENT PRIMARY KEY,
     username VARCHAR(128) NOT NULL UNIQUE,
     pwd_algo VARCHAR(32) NOT NULL,
     pwd_params VARCHAR(64) NOT NULL,
     pwd_salt VARBINARY(32) NOT NULL,
     pwd_hash VARBINARY(128) NOT NULL,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;`,
  (err) => {
    if (err) console.error("Table init error:", err.message);
  }
);

const app = express();
// ...existing code...
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Enhanced security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Cache-Control", "no-store");
  next();
});

// Helper: derive password using scrypt
function derivePassword(password, cb) {
  const salt = crypto.randomBytes(32); // 256-bit salt
  // Stronger scrypt cost than sample; balanced for typical server
  const N = 32768, r = 8, p = 1, keyLen = 64; // 512-bit output
  crypto.scrypt(password, salt, keyLen, { N, r, p }, (err, derived) => {
    if (err) return cb(err);
    cb(null, {
      algo: "scrypt",
      params: `N=${N},r=${r},p=${p},len=${keyLen}`,
      salt,
      hash: derived,
    });
  });
}

// Input validation helpers
function sanitizeUsername(raw) {
  if (typeof raw !== "string") return "";
  const trimmed = raw.trim();
  return /^[A-Za-z0-9_.\-@]{3,64}$/.test(trimmed) ? trimmed : "";
}
function validatePassword(pw) {
  return (
    typeof pw === "string" &&
    pw.length >= 12 &&
    /[A-Z]/.test(pw) &&
    /[a-z]/.test(pw) &&
    /\d/.test(pw) &&
    /[^A-Za-z0-9]/.test(pw)
  );
}

/**
 * register URL
 */
// ...existing code...
app.post("/register", (req, res) => {
  const username = sanitizeUsername(req.body.username);
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!username) {
    return res.status(400).json({ error: "invalid username" });
  }
  if (!validatePassword(password)) {
    return res.status(400).json({ error: "weak password" });
  }

  // Check existence
  pool.query("SELECT id FROM users WHERE username = ? LIMIT 1", [username], (selErr, rows) => {
    if (selErr) {
      console.error("Lookup error:", selErr.message);
      return res.status(500).json({ error: "internal" });
    }
    if (rows && rows.length) {
      return res.status(409).json({ error: "username taken" });
    }

    derivePassword(password, (hashErr, record) => {
      if (hashErr) {
        console.error("Hash error:", hashErr.message);
        return res.status(500).json({ error: "internal" });
      }

      const sql =
        "INSERT INTO users (username, pwd_algo, pwd_params, pwd_salt, pwd_hash) VALUES (?, ?, ?, ?, ?)";
      const params = [
        username,
        record.algo,
        record.params,
        record.salt,
        record.hash,
      ];

      pool.query(sql, params, (insErr) => {
        if (insErr) {
          if (insErr.code === "ER_DUP_ENTRY") {
            return res.status(409).json({ error: "username taken" });
          }
          console.error("Insert error:", insErr.message);
          return res.status(500).json({ error: "internal" });
        }
        return res.status(201).json({ message: "registered" });
      });
    });
  });
});

// ...existing code...
const PORT = process.env.PORT || 3000;
// ...existing code...
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
