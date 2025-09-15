const express = require("express");
const mysql = require("mysql");
const crypto = require('crypto');
require('dotenv').config();

const app = express();

// Security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

// Create a MySQL connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

app.post("/login", async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ success: false, message: "Username and password are required." });
    }

    try {
        // Get user data from database
        await db.promise().query(
            'SELECT id, username, salt, password_hash FROM users WHERE username = ?',
            [username]
        )
        .then(async ([rows]) => {
            if (rows.length === 0) {
                return res.status(401).json({ success: false, message: "Invalid username or password." });
            }

            const user = rows[0];
            
            // Generate hash from incoming password using stored salt
            const derivedKey = crypto.pbkdf2Sync(password, user.salt, 1000, 64, 'sha512');
            const generatedHash = derivedKey.toString('hex');

            if (generatedHash === user.password_hash) {
                // Securely store session or return token
                req.session.user = { id: user.id, username: user.username };
                
                return res.status(200).json({ 
                    success: true, 
                    message: "Login successful!",
                    token: crypto.randomBytes(16).toString('hex')
                });
            } else {
                return res.status(401).json({ success: false, message: "Invalid password." });
            }
        })
        .catch((err) => {
            console.error("Database query failed:", err);
            return res.status(500).json({ 
                success: false, 
                message: "An error occurred during login."
            });
        });

    } catch (err) {
        console.error("Login error:", err);
        res.status(500).json({ 
            success: false, 
            message: "Internal server error during login."
        });
    }
});

// Create a user example in case needed for testing
const createUserQuery = `
INSERT INTO users (username, salt, password_hash) VALUES (?, ?, ?)
`;

const exampleUser = {
    username: 'admin',
    password: 'securepassword123!'
};

if (process.env.NODE_ENV === 'development') {
    // Only create user in development environment if not exists
    db.promise().query(
        `SELECT id FROM users WHERE username = ?`, 
        [exampleUser.username]
    )
    .then(async ([rows]) => {
        if (rows.length === 0) {
            const salt = crypto.randomBytes(64).toString('base64');
            const hash = crypto.pbkdf2Sync(exampleUser.password, salt, 1000, 64, 'sha512').toString('hex');
            
            await db.promise().query(
                createUserQuery,
                [exampleUser.username, salt, hash]
            );
            
            console.log("Example user created successfully.");
        }
    })
    .catch(err => console.error("Error creating example user:", err));
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});