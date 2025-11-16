const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  port: Number(process.env.DB_PORT) || 3306,
  timezone: "Z",
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (
    !username ||
    !password ||
    username.length < 3 ||
    username.length > 64 ||
    password.length < 8 ||
    password.length > 128 ||
    !/^[A-Za-z0-9._-]+$/.test(username)
  ) {
    return res.status(400).send("Invalid request.");
  }

  pool.query(
    "SELECT id, password_hash, password_salt, password_iters, password_algo FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, rows) => {
      if (err) return res.status(500).send("An error occurred.");

      const row = Array.isArray(rows) && rows.length === 1 ? rows[0] : null;

      const storedHashBuf = row
        ? Buffer.isBuffer(row.password_hash)
          ? row.password_hash
          : typeof row.password_hash === "string"
          ? Buffer.from(row.password_hash, "hex")
          : null
        : null;

      const saltBuf = row
        ? Buffer.isBuffer(row.password_salt)
          ? row.password_salt
          : typeof row.password_salt === "string"
          ? Buffer.from(row.password_salt, "hex")
          : Buffer.alloc(16, 0)
        : Buffer.alloc(16, 0);

      const algo =
        row && typeof row.password_algo === "string" && /^[A-Za-z0-9-]+$/.test(row.password_algo)
          ? row.password_algo
          : "sha256";

      const iters =
        row && Number.isInteger(row.password_iters) && row.password_iters > 0 && row.password_iters <= 1000000
          ? row.password_iters
          : 210000;

      const dkLen = storedHashBuf ? storedHashBuf.length : 32;

      crypto.pbkdf2(password, saltBuf, iters, dkLen, algo, (pbkErr, derived) => {
        if (pbkErr) return res.status(500).send("An error occurred.");

        const target = storedHashBuf || Buffer.alloc(dkLen, 0);
        let ok = false;
        try {
          ok = crypto.timingSafeEqual(derived, target);
        } catch {
          ok = false;
        }

        if (row && ok) {
          return res.status(200).send("Logged in.");
        }
        return res.status(401).send("Invalid credentials.");
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
