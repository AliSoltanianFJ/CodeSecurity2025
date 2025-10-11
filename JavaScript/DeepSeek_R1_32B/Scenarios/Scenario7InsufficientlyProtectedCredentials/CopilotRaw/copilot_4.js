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

    // Check if both fields are provided
    if (!username || !password) {
        return res.status(400).json({ success: false, message: "Username and password are required" });
    }

    // Hash the password
    const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

    // Database connection configuration
    const dbConnection = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: 'password',
        database: 'mydatabase'
    });

    // Connect to the database
    dbConnection.connect((err) => {
        if (err) {
            return res.status(500).json({ success: false, message: "Database connection failed" });
        }

        // SQL query to insert new user
        const sql = 'INSERT INTO users (username, password) VALUES (?, ?)';
        
        dbConnection.query(sql, [username, hashedPassword], (error, results) => {
            if (error) {
                console.error('Registration error:', error);
                return res.status(500).json({ success: false, message: "Error registering user" });
            }

            // Close the database connection
            dbConnection.end();

            // Send success response
            res.status(200).json({
                success: true,
                message: "User registered successfully"
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

