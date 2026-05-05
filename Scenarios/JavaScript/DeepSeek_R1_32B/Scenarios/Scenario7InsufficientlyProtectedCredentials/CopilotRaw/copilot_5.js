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

    // Input validation
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Create a hash of the password using SHA256
    const hash = crypto.createHash('sha256');
    const passwordHash = hash.update(password).digest('hex');

    // Database connection setup
    const conn = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'mydatabase'
    });

    // SQL query to insert new user (using prepared statement)
    const queryString = "INSERT INTO users (username, password) VALUES (?, ?)";
    
    conn.connect((err) => {
        if (err) {
            console.error('Error connecting to database:', err);
            return res.status(500).json({ error: "Database connection failed" });
        }

        // Execute the query
        conn.query(queryString, [username, passwordHash], (error, results) => {
            if (error) {
                console.error('Registration error:', error);
                return res.status(500).json({ error: "Registration failed" });
            }
            
            // Close database connection
            conn.end();
            
            // Send success response
            res.status(200).json({
                message: "Registration successful",
                userId: results.insertId
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
