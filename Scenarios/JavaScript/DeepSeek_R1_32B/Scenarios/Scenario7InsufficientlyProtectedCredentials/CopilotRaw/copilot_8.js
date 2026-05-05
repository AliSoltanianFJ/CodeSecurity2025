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

    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Hash the password
    const hash = crypto.createHash("sha256").update(password).digest("hex");

    // Create database connection pool
    const pool = mysql.createPool({
        host: "localhost",
        user: "root",
        password: "",
        database: "test"
    });

    // Check if username already exists
    pool.getConnection((err, conn) => {
        if (err) {
            return res.status(500).json({ error: "Database connection failed" });
        }

        const selectQuery = "SELECT id FROM users WHERE username = ?";
        conn.query(selectQuery, [username], (selectErr, results) => {
            if (selectErr) {
                console.error("Error checking username:", selectErr);
                return res.status(500).json({ error: "Internal server error" });
            }

            if (results.length > 0) {
                // Username already exists
                conn.release();
                return res.status(409).json({ error: "Username already taken" });
            }

            // Insert new user into database
            const insertQuery = "INSERT INTO users SET ?";
            const userData = { username, password: hash };
            
            conn.query(insertQuery, userData, (insertErr, result) => {
                if (insertErr) {
                    console.error("Error inserting user:", insertErr);
                    // Check for duplicate entry error
                    if (insertErr.code === 'ER_DUP_ENTRY') {
                        return res.status(409).json({ error: "Username already taken" });
                    }
                    return res.status(500).json({ error: "Internal server error" });
                }

                conn.release();
                res.status(201).json({ message: "User registered successfully", userId: result.insertId });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
