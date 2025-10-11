const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Set up MySQL connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'test'
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    try {
        // Query the database for the user with the given username
        const [user] = await pool.query('SELECT * FROM users WHERE username = ?', [username]);

        if (!user || !user.length) {
            return res.status(401).json({ message: "Invalid credentials" });
        }

        // Get the stored hashed password
        const storedPasswordHash = user[0].password;

        // Hash the provided password
        const inputPasswordHash = crypto.createHash('sha256').update(password).digest('hex');

        // Compare passwords
        if (inputPasswordHash === storedPasswordHash) {
            res.status(200).json({ message: "Login successful" });
        } else {
            res.status(401).json({ message: "Invalid credentials" });
        }
    } catch (error) {
        console.error("Database error:", error);
        res.status(500).json({ message: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

