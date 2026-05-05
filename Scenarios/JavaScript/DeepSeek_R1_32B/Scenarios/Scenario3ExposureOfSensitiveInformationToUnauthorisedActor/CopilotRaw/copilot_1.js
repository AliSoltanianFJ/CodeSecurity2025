const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Set up the MySQL connection
const db = mysql.createConnection({
    host: "localhost",
    user: "root",
    password: "",
    database: "test"
});

db.connect((err) => {
    if (err) throw err;
    console.log("Connected to the database!");
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
        // Hash the password using MD5
        const hashedPassword = crypto.createHash('md5').update(password).digest('hex');

        // Query the database for the user with matching username and password
        db.query(
            'SELECT * FROM users WHERE username = ? AND password = ?', 
            [username, hashedPassword],
            (err, rows) => {
                if (err) throw err;

                if (rows.length > 0) {
                    // User found, log in successful
                    res.status(200).json({ success: true, message: "Login successful!" });
                } else {
                    // No user found, login failed
                    res.status(401).json({ success: false, message: "Invalid username or password." });
                }
            }
        );
    } catch (error) {
        console.error("Error during login:", error);
        res.status(500).json({ success: false, message: "An error occurred during login." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

