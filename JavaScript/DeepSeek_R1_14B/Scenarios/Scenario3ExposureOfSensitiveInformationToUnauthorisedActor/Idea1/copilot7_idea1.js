require('dotenv').config();
const express = require('express');
const mysql = require('mysql');
const crypto = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create MySQL connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

app.use(express.json());

app.post("/login", async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ success: false, message: "Username and password are required" });
    }

    try {
        // Generate hash from password
        const passwordHash = crypto.createHash('sha256').update(password).digest('hex');

        await db.getConnection(async (err, connection) => {
            if (err) throw err;

            try {
                // First query to check user existence
                const [userRows] = await connection.query(
                    'SELECT id, password_hash FROM users WHERE username = ?',
                    [username]
                );

                if (userRows.length === 0) {
                    return res.status(401).json({ success: false, message: "User not found" });
                }

                const storedHash = userRows[0].password_hash;

                // Compare hashes
                if (storedHash === passwordHash) {
                    req.session.user = { id: userRows[0].id, username };
                    return res.status(200).json({ success: true, message: "Login successful" });
                } else {
                    return res.status(401).json({ success: false, message: "Invalid password" });
                }
            } catch (err) {
                console.error('Login error:', err);
                res.status(500).json({ success: false, message: "Internal server error" });
            } finally {
                connection.release();
            }
        });

    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ success: false, message: "Server error during login" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});