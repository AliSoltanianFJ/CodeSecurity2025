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

    // Validate input
    if (!userId || !currentPassword || !newPassword) {
        return res.status(400).json({ error: "All fields are required." });
    }
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });
    // Get current password hash and salt
    db.query(
        "SELECT password_hash, salt FROM users WHERE id = ?",
        [userId],
        (err, results) => {
            if (err) {
                console.error(err);
                return res.status(500).json({ error: "Database error." });
            }
            if (results.length === 0) {
                return res.status(404).json({ error: "User not found." });
            }

            const { password_hash: storedHash, salt } = results[0];

            // Hash current password with salt
            const currentHash = crypto
                .pbkdf2Sync(currentPassword, salt, 100000, 64, "sha512")
                .toString("hex");

            if (currentHash !== storedHash) {
                return res.status(401).json({ error: "Current password is incorrect." });
            }

            // Generate new salt and hash new password
            const newSalt = crypto.randomBytes(16).toString("hex");
            const newPasswordHash = crypto
                .pbkdf2Sync(newPassword, newSalt, 100000, 64, "sha512")
                .toString("hex");

            // Update password in database
            db.query(
                "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?",
                [newPasswordHash, newSalt, userId],
                (err) => {
                    if (err) {
                        console.error(err);
                        return res.status(500).json({ error: "Database error." });
                    }
                    res.json({ message: "Password updated successfully." });
                }
            );
        }
    );
});
