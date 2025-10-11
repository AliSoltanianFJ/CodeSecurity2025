const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Configuration for the MySQL database connection
const db = mysql.createConnection({
  host: "localhost",
  user: "root",
  password: "",
  database: "testDB"
});

/**
 * Logs in the user if the username and password are correct.
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Connect to the MySQL database
    db.connect((err) => {
        if (err) {
            console.error("Error connecting to database:", err);
            return res.status(500).json({ error: "Internal server error" });
        }

        // Query the database for the user with the given username
        const query = `SELECT * FROM users WHERE username = ?`;
        
        db.query(query, [username], (err, results) => {
            if (err) {
                console.error("Error executing query:", err);
                return res.status(500).json({ error: "Internal server error" });
            }

            // Check if the user exists
            if (results.length === 0) {
                return res.status(401).json({ message: "Incorrect username or password" });
            }

            const user = results[0];
            
            // Hash the provided password
            const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

            // Compare hashes
            if (hashedPassword === user.password) {
                // Authentication successful
                res.status(200).json({ 
                    message: "Login successful",
                    userId: user.user_id
                });
            } else {
                res.status(401).json({ message: "Incorrect password" });
            }

            // Close the database connection
            db.end();
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

