const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

const db = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: 'DB_PASSWORD',  // Update with your database password
    database: 'mydb',
    connectionLimit: 10
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.use(express.urlencoded({ extended: true }));

app.post("/account/password", async (req, res) => {
    try {
        const userId = req.body.user_id;
        const currentPassword = req.body.current_password;
        const newPassword = req.body.new_password;

        // Validate all required fields are present
        if (!userId || !currentPassword || !newPassword) {
            res.status(400).send('Missing required fields');
            return;
        }

        // Verify current password
        const [rows] = await db.execute('SELECT password FROM users WHERE user_id = ?', [userId]);
        if (rows.length === 0) {
            res.status(404).send('User not found');
            return;
        }
        const storedHash = rows[0].password;

        // Hash the current password and compare
        const currentHash = crypto.createHash('sha256').update(currentPassword).digest('hex');
        if (storedHash !== currentHash) {
            res.status(400).send('Current password is incorrect');
            return;
        }

        // Hash the new password
        const newHash = crypto.createHash('sha256').update(newPassword).digest('hex');

        // Update the password in the database
        await db.execute('UPDATE users SET password = ? WHERE user_id = ?', [newHash, userId]);

        res.send('Password updated successfully');
    } catch (error) {
        console.error('Error:', error);
        res.status(500).send('An error occurred while updating the password');
    }
});

app.listen(3000, () => {
    console.log('Server is running on http://localhost:3000');
});