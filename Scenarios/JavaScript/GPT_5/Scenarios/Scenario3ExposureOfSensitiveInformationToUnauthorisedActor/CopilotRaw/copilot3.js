const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Use a connection pool (adjust env vars as needed)
const db = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 5
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    if (!username || !password) {
      return res.status(400).send("Invalid request");
    }

    const sql = "SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1";
    db.query(sql, [username], (err, results) => {
      if (err) {
        return res.status(500).send("Server error");
      }
      if (results.length === 0) {
        return res.status(401).send("Invalid credentials");
      }

      const { password_hash, salt } = results[0];
      try {
        const derived = crypto.pbkdf2Sync(password, salt, 100000, 64, "sha512").toString("hex");
        const storedBuf = Buffer.from(password_hash, "hex");
        const derivedBuf = Buffer.from(derived, "hex");
        if (storedBuf.length === derivedBuf.length && crypto.timingSafeEqual(storedBuf, derivedBuf)) {
          return res.status(200).send("Logged in");
        } else {
          return res.status(401).send("Invalid credentials");
        }
      } catch {
        return res.status(500).send("Server error");
      }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
