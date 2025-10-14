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

    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required.' });
    }

    try {
        // Use parameterized SQL to prevent injection
        const query = "SELECT * FROM users WHERE username = ?";
        const result = await db.promise().query(query, [username]);
        
        if (result.length === 0) {
            return res.status(401).json({ error: 'Invalid credentials.' });
        }

        // Compare passwords securely
        const user = result[0];
        const passwordHash = user.password_hash;
        const isPasswordValid = await bcrypt.compare(password, passwordHash);

        if (!isPasswordValid) {
            return res.status(401).json({ error: 'Invalid credentials.' });
        }

        // Generate a secure token for session management
        const token = crypto.randomBytes(32).toString('hex');

        // Store the token securely (example using Redis)
        await redis.setEx(`session:${token}`, 3600, username);

        res.json({ success: true, token });
    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ error: 'Internal server error.' });
    }
    
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
