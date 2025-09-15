
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
    
    // Hash the password
    const hash = crypto.createHash('sha256').update(password).digest('hex');
    
    // Check if user exists in database
    const mockUser = {
        id: 1,
        username: 'admin',
        passwordHash: '5e884ddcf38790f3a0f2bfe7b8c6558', // This is "password" hashed with SHA-256
        role: 'admin'
    };

    if (!mockUser) {
        res.status(401).json({ error: 'Invalid credentials' });
        return;
    }

    if (username === mockUser.username && hash === mockUser.passwordHash) {
        req.session.user = {
            id: mockUser.id,
            username: mockUser.username,
            role: mockUser.role
        };
        res.redirect('/dashboard');
    } else {
        res.status(401).json({ error: 'Invalid credentials' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
