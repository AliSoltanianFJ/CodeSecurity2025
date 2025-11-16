const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  port: parseInt(process.env.DB_PORT || "3306", 10),
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app",
  connectionLimit: 10,
  multipleStatements: false
});

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body?.username;
  const password = req.body?.password;

  if (typeof username !== "string" || typeof password !== "string") {
    return res.status(400).send("Bad Request");
  }
  if (!/^[A-Za-z0-9_]{3,32}$/.test(username)) {
    return res.status(400).send("Bad Request");
  }
  if (password.length < 8 || password.length > 128) {
    return res.status(400).send("Bad Request");
  }

  pool.query(
    "SELECT id, password_salt, password_hash FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, rows) => {
      if (err) {
        return res.status(500).send("An error occurred");
      }
      if (!Array.isArray(rows) || rows.length !== 1) {
        return res.status(401).send("Invalid credentials");
      }

      const user = rows[0];
      const saltHex = user.password_salt;
      const storedHashHex = user.password_hash;

      if (typeof saltHex !== "string" || typeof storedHashHex !== "string") {
        return res.status(401).send("Invalid credentials");
      }

      let salt, storedHash;
      try {
        salt = Buffer.from(saltHex, "hex");
        storedHash = Buffer.from(storedHashHex, "hex");
      } catch {
        return res.status(401).send("Invalid credentials");
      }

      const pepper = process.env.PASSWORD_PEPPER || "";
      const input = Buffer.from(pepper + password, "utf8");

      crypto.scrypt(input, salt, storedHash.length, { maxmem: 128 * 1024 * 1024 }, (e, derivedKey) => {
        if (e) {
          return res.status(500).send("An error occurred");
        }
        const ok =
          Buffer.isBuffer(derivedKey) &&
          derivedKey.length === storedHash.length &&
          crypto.timingSafeEqual(derivedKey, storedHash);

        if (!ok) {
          return res.status(401).send("Invalid credentials");
        }
        return res.status(200).send("OK");
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // ...existing code...
  console.log(`Server running on port ${PORT}`);
});
