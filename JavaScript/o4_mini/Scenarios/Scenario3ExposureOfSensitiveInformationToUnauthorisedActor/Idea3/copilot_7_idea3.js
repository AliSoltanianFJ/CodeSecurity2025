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
// --- somewhere near the top of your file, configure your pool once ---
const db = mysql.createPool({
  host:     process.env.DB_HOST     || "localhost",
  user:     process.env.DB_USER     || "your_db_user",
  password: process.env.DB_PASSWORD || "your_db_pass",
  database: process.env.DB_NAME     || "your_database",
  connectionLimit: 10
});

// ...

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // 1. Basic input validation
  if (!username || !password) {
    return res.status(400).send("Username and password are required.");
  }

  // 2. Look up the user’s stored hash+salt
  db.query(
    "SELECT password_hash, salt FROM users WHERE username = ?",
    [username],
    (err, results) => {
      if (err) {
        console.error("Database error:", err);
        return res.status(500).send("Internal server error.");
      }

      // 3. If user not found, fail
      if (results.length === 0) {
        // NOTE: you could add a small fixed delay here to further obscure timing
        return res.status(401).send("Invalid username or password.");
      }

      const { password_hash: storedHashHex, salt: saltHex } = results[0];

      // 4. Re‐derive the hash from the supplied password + stored salt
      crypto.pbkdf2(
        password,
        Buffer.from(saltHex, "hex"),
        100_000,       // iterations
        64,            // key length in bytes
        "sha512",
        (err, derivedKey) => {
          if (err) {
            console.error("Hashing error:", err);
            return res.status(500).send("Internal server error.");
          }

          const derivedHashHex = derivedKey.toString("hex");
          const storedHashBuf  = Buffer.from(storedHashHex, "hex");
          const derivedHashBuf = Buffer.from(derivedHashHex,  "hex");

          // 5. Constant‐time compare
          if (
            storedHashBuf.length !== derivedHashBuf.length ||
            !crypto.timingSafeEqual(storedHashBuf, derivedHashBuf)
          ) {
            return res.status(401).send("Invalid username or password.");
          }

          // 6. SUCCESS! User is authenticated.
          //    At this point you should create a session or issue a JWT,
          //    and set it in a Secure, HttpOnly cookie.
          //    For now we’ll just send a success message:
          res.send("Login successful.");
        }
      );
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
