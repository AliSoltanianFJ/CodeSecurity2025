const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
const db = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 10
});

app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // 1) Basic input validation
  if (!username || !password) {
    return res.status(400).send("Username and password are required");
  }

  // 2) Lookup user by username
  const sql = "SELECT id, password_hash, salt, iterations FROM users WHERE username = ?";
  db.query(sql, [username], (err, results) => {
    if (err) {
      console.error("Database error:", err);
      return res.status(500).send("Internal server error");
    }

    // If user not found, do a dummy PBKDF2 to keep timing similar
    if (results.length !== 1) {
      crypto.pbkdf2(password, "0000000000000000", 100000, 64, "sha512", () => {
        return res.status(401).send("Invalid username or password");
      });
      return;
    }

    const { id, password_hash, salt, iterations } = results[0];

    // 3) Derive a key from the supplied password using the same salt & iterations
    crypto.pbkdf2(password, Buffer.from(salt, "hex"), iterations, 64, "sha512", (err, derivedKey) => {
      if (err) {
        console.error("PBKDF2 error:", err);
        return res.status(500).send("Internal server error");
      }

      const storedHash = Buffer.from(password_hash, "hex");

      // 4) Constant-time comparison
      if (derivedKey.length !== storedHash.length ||
          !crypto.timingSafeEqual(derivedKey, storedHash)) {
        return res.status(401).send("Invalid username or password");
      }

      // 5) At this point credentials are valid.
      //    If you have express-session set up, you can do:
      //      req.session.userId = id;
      //      res.send("Login successful");
      //    Otherwise return a token or a success message:

      res.send("Login successful");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
