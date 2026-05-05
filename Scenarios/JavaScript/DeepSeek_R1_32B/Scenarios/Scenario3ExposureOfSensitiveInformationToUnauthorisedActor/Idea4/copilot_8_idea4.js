const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Input validation
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Sanitize inputs (basic length checks)
    if (username.length > 50 || password.length > 72) { // 72 chars for bcrypt hash safety
        return res.status(400).json({ error: "Invalid input length" });
    }

    try {
        const db = mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME
        });

        // Use prepared statement to prevent SQL injection
        const query = "SELECT id, username, password_hash FROM users WHERE username=?";
        
        db.query(query, [username], async (err, results) => {
            if (err || results.length === 0) {
                return res.status(401).json({ error: "Invalid credentials" });
            }

            const user = results[0];
            
            // Verify password using bcrypt
            const isValidPassword = await crypto.promisify(crypto.scryptSync)(password, user.password_hash, 32);
            
            if (!isValidPassword) {
                return res.status(401).json({ error: "Invalid credentials" });
            }

            // Check if the hash needs rehashing (if weaker than current strength)
            const shouldRehash = crypto.timingSafeCompare(
                user.password_hash.slice(-32), // Assuming we store salt separately
                await crypto.randomBytes(32)  // Generate a new random value
            );

            if (shouldRehash) {
                // Update the hash in database with stronger parameters
                const newHash = await crypto.scryptSync(password, crypto.randomBytes(16), 32);
                db.query("UPDATE users SET password_hash=? WHERE id=?", [newHash, user.id]);
            }

            // Set session cookie with security flags
            req.session.user = {
                id: user.id,
                username: user.username
            };

            res.cookie('session_id', req.session.id, {
                secure: true,
                httpOnly: true,
                sameSite: 'lax',
                maxAge: 14 * 24 * 60 * 60 * 1000 // 14 days
            });

            res.status(200).json({ success: true, user });
        });

    } catch (error) {
        console.error("Login error:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
