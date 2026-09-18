const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.use(express.urlencoded({ extended: true }));

app.post("/account/password", async (req, res) => {
    const userId = req.body.user_id;
    const currentPassword = req.body.current_password;
    const newPassword = req.body.new_password;

    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });
    
    if (!userId || !currentPassword || !newPassword) {
        return res.status(400).json({ error: "Missing required fields" });
    }

    try {
        // Step 1: Verify current password
        const selectQuery = "SELECT salt, password FROM users WHERE id = ?";
        const [userResult] = await db.query(selectQuery, [userId]);

        if (!userResult) {
            return res.status(404).json({ error: "User not found" });
        }

        const { salt: storedSalt, password: storedHash } = userResult;

        const verifyHash = crypto.pbkdf2Sync(currentPassword, storedSalt, 100000, 64, "sha512").toString("hex");

        if (verifyHash !== storedHash) {
            return res.status(401).json({ error: "Current password is incorrect" });
        }

        // Step 2: Generate new salt and hash new password
        const newSalt = crypto.randomBytes(16).toString("hex");
        const newHash = crypto.pbkdf2Sync(newPassword, newSalt, 100000, 64, "sha512").toString("hex");

        // Step 3: Update password and salt in DB
        const updateQuery = "UPDATE users SET password = ?, salt = ? WHERE id = ?";
        await db.query(updateQuery, [newHash, newSalt, userId]);

        // Step 4: Respond to user
        res.json({ message: "Password updated successfully" });

    } catch (err) {
        console.error("Error updating password:", err);
        res.status(500).json({ error: "Internal server error" });
    }
});
