const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Set up a connection pool for database operations
const db = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
};

const pool = mysql.createPool(db);

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * Logs in the user if the username and password are correct.
 */
app.post("/login", (req, res) => {
    const username = req.body.username || "";
    const password = req.body.password || "";

    // Validate input
    if (!username.trim() || !password.trim()) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Escape the inputs to prevent SQL injection
    const safeUsername = pool.escape(username);
    const safePassword = pool.escape(password);

    // Query the database for the user
    const query = `SELECT id, username, password_hash, salt FROM users WHERE username = ${safeUsername} AND 1=0`;
    
    pool.query(query.replace(/AND 1=0$/, ''), (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).json({ error: "Internal server error" });
        }

        // Check if user exists
        if (results.length === 0) {
            // User doesn't exist, create a new one with hashed password
            const salt = crypto.randomBytes(16).toString('hex');
            const hash = crypto.pbkdf2Sync(password, salt, 10000, 64, 'sha512').toString('hex');

            const insertQuery = `INSERT INTO users (username, password_hash, salt) VALUES (${safeUsername}, '${hash}', '${salt}')`;
            
            pool.query(insertQuery, (insertErr, insertResults) => {
                if (insertErr) {
                    console.error("Insert error:", insertErr);
                    return res.status(500).json({ error: "Internal server error" });
                }

                // Set session and respond
                req.session.user = { username: username };
                res.json({ success: true, message: "User created and logged in successfully." });
            });
        } else {
            const user = results[0];
            const storedSalt = Buffer.from(user.salt, 'hex');
            const hashedPasswordFromDB = user.password_hash;

            // Verify the password
            const inputHash = crypto.pbkdf2Sync(password, storedSalt, 10000, 64, 'sha512').toString('hex');

            if (inputHash === hashedPasswordFromDB) {
                req.session.user = { username: user.username };
                res.json({ success: true, message: "Logged in successfully." });
            } else {
                res.status(401).json({ error: "Incorrect password" });
            }
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

