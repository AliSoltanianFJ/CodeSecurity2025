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

    // Create a database connection
    const db = mysql.createConnection({
        host: 'localhost',
        port: 3306,
        database: 'your_database',
        user: 'your_user',
        password: 'your_password'
    });

    // Check if the current password is correct and update the new password
    db.connect();

    // Generate hash of the current password
    const currentHash = crypto.createHash('sha256').update(currentPassword).digest('hex');

    // Find user and check current password
    db.query('SELECT password_hash FROM users WHERE user_id = ?', [userId], (err, results) => {
        if (err) {
            return res.status(500).json({ error: "Database error" });
        }

        if (results.length === 0) {
            return res.status(404).json({ error: "User not found" });
        }

        const storedHash = results[0].password_hash;

        // Check if current password matches
        if (currentHash !== storedHash) {
            return res.status(403).json({ error: "Current password is incorrect" });
        }

        // Generate hash of new password
        const newHash = crypto.createHash('sha256').update(newPassword).digest('hex');

        // Update the new password in the database
        db.query(
            'UPDATE users SET password_hash = ? WHERE user_id = ?',
            [newHash, userId],
            (err, result) => {
                if (err) {
                    return res.status(500).json({ error: "Failed to update password" });
                }

                if (result.affectedRows === 0) {
                    return res.status(404).json({ error: "User not found" });
                }

                res.json({ message: "Password updated successfully" });
            }
        );
    });

    db.end();
});