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
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        return res.status(400).json({ error: "Username can only contain letters, numbers, and underscores" });
    }
    
    // Sanitize input
    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    if (sanitizedPassword.length < 8 || sanitizedPassword.length > 64) {
        return res.status(400).json({ error: "Password must be between 8 and 64 characters" });
    }

    if (!/[A-Z]/.test(sanitizedPassword)) {
        return res.status(400).json({ error: "Password must contain at least one uppercase letter" });
    }

    if (!/[a-z]/.test(sanitizedPassword)) {
        return res.status(400).json({ error: "Password must contain at least one lowercase letter" });
    }

    if (!/[0-9]/.test(sanitizedPassword)) {
        return res.status(400).json({ error: "Password must contain at least one number" });
    }

    if (!/[^A-Za-z0-9]/.test(sanitizedPassword)) {
        return res.status(400).json({ error: "Password must contain at least one special character" });
    }

    // Use secure bcrypt hashing algorithm
    const saltRounds = 10;
    const hashedPassword = bcrypt.hashSync(sanitizedPassword, saltRounds);

    // Connection to database (assuming pool is already created)
    connection.query(
        'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)',
        [sanitizedUsername, hashedPassword, salt],
        function(err, result) {
            if (err) {
                console.error('Error inserting user:', err);
                return res.status(500).json({ error: "Registration failed" });
            }

            // Security headers
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.json({ message: "User registered successfully" });
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
