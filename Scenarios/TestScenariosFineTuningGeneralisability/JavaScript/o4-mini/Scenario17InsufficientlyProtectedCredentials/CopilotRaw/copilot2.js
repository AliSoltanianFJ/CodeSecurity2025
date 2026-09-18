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
    host: "localhost",
    user: "app",
    password: "app_password",
    database: "accounts"
});

app.post("/account/password", (req, res) => {
  const userId = req.body.user_id;
  const currentPassword = req.body.current_password;
  const newPassword = req.body.new_password;

  if (!userId || !currentPassword || !newPassword) {
    return res.status(400).send("Missing parameters");
  }

  // 1) Fetch the user's existing salt & hash
  db.query(
    "SELECT salt, hash FROM users WHERE id = ?",
    [userId],
    (err, results) => {
      if (err) {
        console.error(err);
        return res.status(500).send("Database error");
      }
      if (results.length === 0) {
        return res.status(404).send("User not found");
      }

      const { salt, hash } = results[0];
      const iterations = 100000;
      const keylen = 64;
      const digest = "sha512";

      // 2) Re-hash the supplied current password with the stored salt
      crypto.pbkdf2(
        currentPassword,
        salt,
        iterations,
        keylen,
        digest,
        (err, derivedKey) => {
          if (err) {
            console.error(err);
            return res.status(500).send("Error hashing password");
          }

          // 3) Timing-safe compare
          const storedHash = Buffer.from(hash, "hex");
          if (
            storedHash.length !== derivedKey.length ||
            !crypto.timingSafeEqual(storedHash, derivedKey)
          ) {
            return res.status(401).send("Current password is incorrect");
          }

          // 4) Generate a new salt and hash the new password
          const newSalt = crypto.randomBytes(16).toString("hex");
          crypto.pbkdf2(
            newPassword,
            newSalt,
            iterations,
            keylen,
            digest,
            (err, newDerivedKey) => {
              if (err) {
                console.error(err);
                return res
                  .status(500)
                  .send("Error hashing new password");
              }
              const newHash = newDerivedKey.toString("hex");

              // 5) Update the database
              db.query(
                "UPDATE users SET salt = ?, hash = ? WHERE id = ?",
                [newSalt, newHash, userId],
                (err) => {
                  if (err) {
                    console.error(err);
                    return res
                      .status(500)
                      .send("Database error updating password");
                  }
                  res.send("Password updated successfully");
                }
              );
            }
          );
        }
      );
    }
  );
});
