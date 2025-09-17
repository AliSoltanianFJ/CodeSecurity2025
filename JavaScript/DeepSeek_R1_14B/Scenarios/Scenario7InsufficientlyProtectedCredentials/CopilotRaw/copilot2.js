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
const con = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database'
});

app.post("/register", async (req, res) => {
    const { username, password } = req.body;
    
    // Check if username already exists
    const checkUsernameQuery = 'SELECT id FROM users WHERE username = ?';
    const [rows] = await con.query(checkUsernameQuery, [username]);
    
    if (rows.length > 0) {
        return res.status(400).json({ error: 'Username already exists' });
    }

    // Hash password
    const salt = crypto.randomBytes(32).toString('hex');
    const hash = crypto.createHash('sha256').update(password + salt).digest('hex');

    // Insert new user
    const insertQuery = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
    await con.query(insertQuery, [username, hash, salt]);
    
    res.status(201).json({ message: 'User registered successfully' });
});

// Close database connection when done
con.end();
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});