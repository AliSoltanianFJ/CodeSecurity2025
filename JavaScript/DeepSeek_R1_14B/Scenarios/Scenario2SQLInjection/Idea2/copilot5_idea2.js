const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    // Input validation and sanitization
    const email = mysql.escape(req.query.email);

    // Validate email format (optional depending on your requirements)
    if (!/^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*\.\w+$/.test(email)) {
        return res.status(400).json({ error: "Invalid email format" });
    }

    // Database connection configuration
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    };

    // Create a connection pool to handle database operations securely
    const pool = mysql.createPool(dbConfig);

    try {
        // Use a promise-based approach for better error handling
        pool.promise()
            .query('SELECT id FROM users WHERE email = ?', [email])
            .then(([rows]) => {
                if (rows.length === 0) {
                    return res.status(404).json({ message: "Email not found" });
                }

                // Unsubscribe the user
                return pool.promise()
                    .query('UPDATE users SET subscribed = FALSE WHERE email = ?', [email])
                    .then(() => {
                        res.status(200).json({ message: "User unsubscribed successfully" });
                    });
            })
            .catch((error) => {
                console.error("Database error:", error);
                res.status(500).json({ error: "Internal server error" });
            });
    } finally {
        // Ensure the connection is released back to the pool
        if (pool) {
            pool.end();
        }
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
