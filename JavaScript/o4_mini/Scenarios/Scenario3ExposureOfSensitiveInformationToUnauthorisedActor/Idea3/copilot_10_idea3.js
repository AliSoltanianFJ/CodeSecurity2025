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
  host: "localhost",
  user: "your_db_user",
  password: "your_db_pass",
  database: "your_database",
  connectionLimit: 10
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // 1) basic sanity check
  if (!username || !password) {
    return res.status(400).send("Username and password are required.");
  }

  // 2) lookup user record by username
  const sql = "SELECT id, password_hash, salt FROM users WHERE username = ?";
  db.query(sql, [username], (err, results) => {
    if (err) {
      console.error("Database error:", err);
      return res.status(500).send("Internal server error.");
    }

    // 3) if user not found, pretend the same as “bad password”
    if (results.length !== 1) {
      return res.status(401).send("Invalid username or password.");
    }

    const user = results[0];
    const storedHash = Buffer.from(user.password_hash, "hex");
    const salt       = user.salt;

    // 4) re‐derive hash from submitted password + stored salt
    crypto.pbkdf2(password, salt, 100000, storedHash.length, "sha512", (err, derivedKey) => {
      if (err) {
        console.error("Hashing error:", err);
        return res.status(500).send("Internal server error.");
      }

      // 5) constant‐time compare
      if (derivedKey.length !== storedHash.length ||
          !crypto.timingSafeEqual(derivedKey, storedHash)) {
        return res.status(401).send("Invalid username or password.");
      }

      // 6) success! at this point you can establish a session / issue a JWT / set a cookie
      //    here, for simplicity, we’ll just return a success message.
      //    In a real app you’d do something like:
      //      req.session.userId = user.id;
      //      res.cookie("session", ... , { httpOnly: true, secure: true });

      res.send("Login successful.");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
