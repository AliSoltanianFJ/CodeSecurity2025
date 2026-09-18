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

const accountRateLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  message: "Too many requests. Please try again later."
});

app.post("/account/password", accountRateLimiter, (req, res) => {
  const userId = req.body.user_id;
  const currentPassword = req.body.current_password;
  const newPassword = req.body.new_password;

  // 1) basic new‐password policy
  if (typeof newPassword !== "string" || newPassword.length < 8) {
    return res.status(400).send("New password must be at least 8 characters");
  }

  // 2) fetch the stored salt & hash for this user
  db.query(
    "SELECT password_hash, salt FROM users WHERE id = ?",
    [userId],
    (err, results) => {
      if (err) {
        console.error(err);
        return res.status(500).send("Server error");
      }
      if (results.length === 0) {
        // user not found
        return res.status(404).send("User not found");
      }

      const { password_hash: storedHash, salt: storedSalt } = results[0];
      const iterations = 100000;
      const keylen = 64;
      const digest = "sha512";

      // 3) hash the supplied currentPassword with the stored salt
      crypto.pbkdf2(
        currentPassword,
        storedSalt,
        iterations,
        keylen,
        digest,
        (err, derivedKey) => {
          if (err) {
            console.error(err);
            return res.status(500).send("Server error");
          }

          // 4) timing-safe compare
          const currentHash = derivedKey.toString("hex");
          const match = crypto.timingSafeEqual(
            Buffer.from(currentHash, "hex"),
            Buffer.from(storedHash, "hex")
          );
          if (!match) {
            return res.status(401).send("Current password is incorrect");
          }

          // 5) generate a new salt & hash the newPassword
          crypto.randomBytes(16, (err, saltBuf) => {
            if (err) {
              console.error(err);
              return res.status(500).send("Server error");
            }
            const newSalt = saltBuf.toString("hex");
            crypto.pbkdf2(
              newPassword,
              newSalt,
              iterations,
              keylen,
              digest,
              (err, newDerivedKey) => {
                if (err) {
                  console.error(err);
                  return res.status(500).send("Server error");
                }
                const newHash = newDerivedKey.toString("hex");

                // 6) store the new salt + hash
                db.query(
                  "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?",
                  [newHash, newSalt, userId],
                  (err) => {
                    if (err) {
                      console.error(err);
                      return res.status(500).send("Server error");
                    }
                    res.send("Password updated successfully");
                  }
                );
              }
            );
          });
        }
      );
    }
  );
});