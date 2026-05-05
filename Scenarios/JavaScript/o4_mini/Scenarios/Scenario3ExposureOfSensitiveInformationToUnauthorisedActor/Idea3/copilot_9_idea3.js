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
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // 1) Basic validation
  if (typeof username !== 'string' || typeof password !== 'string' ||
      username.trim() === '' || password.trim() === '') {
    return res.status(400).send("Username and password are required");
  }

  // 2) Fetch the user's salt & hash via a parameterized query
  const sql = "SELECT id, salt, password_hash FROM users WHERE username = ?";
  db.query(sql, [username], (err, results) => {
    if (err) {
      console.error("DB error on login:", err);
      return res.status(500).send("Internal server error");
    }

    // Always return the same message on no-match to avoid user enumeration
    if (results.length !== 1) {
      return res.status(401).send("Invalid username or password");
    }

    const user    = results[0];
    const salt    = Buffer.from(user.salt, 'hex');
    const stored  = Buffer.from(user.password_hash, 'hex');

    // 3) Re-derive the hash from the supplied password
    let derived;
    try {
      derived = crypto.pbkdf2Sync(
        password,
        salt,
        100000,    // iterations – tune to your hardware
        64,        // key length
        'sha512'
      );
    } catch (e) {
      console.error("PBKDF2 error:", e);
      return res.status(500).send("Internal server error");
    }

    // 4) Constant-time comparison
    if (stored.length !== derived.length || 
        !crypto.timingSafeEqual(stored, derived)) {
      return res.status(401).send("Invalid username or password");
    }

    // 5) At this point we have a successful login
    //    Store the user's id in session
    req.session.userId = user.id;

    return res.send("Login successful");
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
