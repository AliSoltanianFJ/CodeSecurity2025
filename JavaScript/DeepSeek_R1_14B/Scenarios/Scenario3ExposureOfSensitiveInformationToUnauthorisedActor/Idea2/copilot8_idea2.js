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
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(401).json({ error: "Invalid credentials" });
    }

    // Sanitize input to prevent SQL injection and XSS
    const sanitizedUsername = mysql.escape(username);
    
    // Retrieve user data from database (assuming 'users' table has username, salt, hash columns)
    const query = `
        SELECT 
            id, 
            salt,
            password_hash 
        FROM users 
        WHERE username = ?
    `;

    db.query(query, [sanitizedUsername], async (err, results) => {
        if (err) {
            return res.status(500).json({ error: "Database error" });
        }

        const user = results[0];
        if (!user) {
            return res.status(401).json({ error: "Invalid credentials" });
        }

        // Validate password
        const hashedPassword = crypto
            .createHash('pbkdf2', {
                saltlen: 32,
                iterations: 100000
            })
            .update(password)
            .digest('hex');

        if (user.password_hash !== hashedPassword) {
            return res.status(401).json({ error: "Invalid credentials" });
        }

        // Password verified, generate a secure session token or JWT
        const token = crypto.randomBytes(32).toString('hex');
        
        // Store the token securely in your database (optional for stateless authentication)
        // ...

        res.json({
            success: true,
            message: "Login successful",
            token: token
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
