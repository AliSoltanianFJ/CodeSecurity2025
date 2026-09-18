const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.use(express.urlencoded({ extended: true }));

const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

const rateLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  message: "Too many requests. Please try again later."
});

app.post("/account/password", rateLimiter, (req, res) => {
  const userId = req.body.user_id;
  const currentPassword = req.body.current_password;
  const newPassword = req.body.new_password;

  // Basic validation
  if (!userId || !currentPassword || !newPassword) {
    return res.status(400).send("Missing parameters.");
  }
  if (newPassword.length < 8) {
    return res.status(400).send("New password must be at least 8 characters.");
  }

  // 1) Look up the user’s existing salt+hash
  const selectSql = "SELECT password_hash, salt FROM users WHERE id = ?";
  db.query(selectSql, [userId], (err, results) => {
    if (err) {
      console.error(err);
      return res.status(500).send("Database error.");
    }
    if (results.length === 0) {
      return res.status(404).send("User not found.");
    }

    const { salt: oldSalt, password_hash: oldHash } = results[0];

    // 2) Hash the supplied currentPassword with the stored salt
    crypto.pbkdf2(
      currentPassword,
      oldSalt,
      100_000,    // iterations
      64,         // key length
      "sha512",   // digest
      (err, derivedKey) => {
        if (err) {
          console.error(err);
          return res.status(500).send("Hashing error.");
        }

        if (derivedKey.toString("hex") !== oldHash) {
          // Password mismatch
          return res.status(401).send("Current password is incorrect.");
        }

        // 3) Generate a brand new salt + hash for the new password
        const newSalt = crypto.randomBytes(16).toString("hex");
        crypto.pbkdf2(
          newPassword,
          newSalt,
          100_000,
          64,
          "sha512",
          (err, newDerivedKey) => {
            if (err) {
              console.error(err);
              return res.status(500).send("Hashing error.");
            }

            const newHash = newDerivedKey.toString("hex");

            // 4) Update the database
            const updateSql =
              "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
            db.query(
              updateSql,
              [newHash, newSalt, userId],
              (err, updateResult) => {
                if (err) {
                  console.error(err);
                  return res.status(500).send("Database error.");
                }
                res.send("Password updated successfully.");
              }
            );
          }
        );
      }
    );
  });
});
