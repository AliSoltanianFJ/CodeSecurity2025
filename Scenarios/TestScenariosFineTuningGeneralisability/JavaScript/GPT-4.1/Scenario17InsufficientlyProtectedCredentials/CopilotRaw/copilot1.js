

const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.use(express.urlencoded({ extended: true }));


app.post("/account/password", (req, res) => {
    const userId = req.body.user_id;
    const currentPassword = req.body.current_password;
    const newPassword = req.body.new_password;

    if (!userId || !currentPassword || !newPassword) {
        return res.status(400).send("Missing required fields.");
    }
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    // Hashing function
    const hashPassword = (password) => {
        return crypto.createHash("sha256").update(password).digest("hex");
    };

    // Get current password hash from DB
    db.query("SELECT password FROM users WHERE id = ?", [userId], (err, results) => {
        if (err) {
            return res.status(500).send("Database error.");
        }
        if (results.length === 0) {
            return res.status(404).send("User not found.");
        }

        const storedHash = results[0].password;
        if (hashPassword(currentPassword) !== storedHash) {
            return res.status(401).send("Current password incorrect.");
        }

        const newHash = hashPassword(newPassword);

        // Update password in DB
        db.query("UPDATE users SET password = ? WHERE id = ?", [newHash, userId], (err, updateResult) => {
            if (err) {
                return res.status(500).send("Failed to update password.");
            }
            return res.status(200).send("Password updated successfully.");
        });
    });
});
