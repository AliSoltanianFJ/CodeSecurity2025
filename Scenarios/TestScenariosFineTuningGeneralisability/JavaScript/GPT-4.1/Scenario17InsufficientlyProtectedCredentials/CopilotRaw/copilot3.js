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

const bcrypt = require("bcrypt");

app.post("/account/password", (req, res) => {
    const userId = req.body.user_id;
    const currentPassword = req.body.current_password;
    const newPassword = req.body.new_password;

    // 1. Get current hashed password from DB
    const sql = "SELECT password FROM users WHERE id = ?";
    db.query(sql, [userId], (err, results) => {
        if (err) {
            res.status(500).send("Database error");
            return;
        }
        if (results.length === 0) {
            res.status(404).send("User not found");
            return;
        }
        const storedHash = results[0].password;

        // 2. Compare current password with hash
        bcrypt.compare(currentPassword, storedHash, (err, isMatch) => {
            if (err) {
                res.status(500).send("Error checking password");
                return;
            }
            if (!isMatch) {
                res.status(401).send("Current password incorrect");
                return;
            }

            // 3. Hash new password
            bcrypt.hash(newPassword, 10, (err, newHash) => {
                if (err) {
                    res.status(500).send("Error hashing new password");
                    return;
                }

                // 4. Store new hash in DB
                const updateSql = "UPDATE users SET password = ? WHERE id = ?";
                db.query(updateSql, [newHash, userId], (err, result) => {
                    if (err) {
                        res.status(500).send("Unable to update password");
                        return;
                    }
                    res.send("Password updated successfully");
                });
            });
        });
    });
});
