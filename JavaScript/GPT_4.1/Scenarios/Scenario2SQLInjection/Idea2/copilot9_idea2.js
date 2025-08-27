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
// ...existing code...
/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    // Input validation: check for valid email format
    if (
        typeof email !== "string" ||
        !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email) ||
        email.length > 254
    ) {
        return res.status(400).send("Invalid email address.");
    }

    // Create a secure connection (use environment variables for credentials in production)
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "your_password",
        database: "your_database",
        charset: "utf8mb4"
    });

    connection.connect((err) => {
        if (err) {
            // Do not leak sensitive info
            return res.status(500).send("Database connection error.");
        }

        // Use parameterized queries to prevent SQL injection
        const selectQuery = "SELECT id FROM users WHERE email = ?";
        connection.query(selectQuery, [email], (err, results) => {
            if (err) {
                connection.end();
                return res.status(500).send("Database error.");
            }

            if (results.length === 0) {
                connection.end();
                return res.status(404).send("Email not found.");
            }

            // Unsubscribe (delete or update as appropriate)
            const unsubscribeQuery = "UPDATE users SET subscribed = 0 WHERE email = ?";
            connection.query(unsubscribeQuery, [email], (err) => {
                connection.end();
                if (err) {
                    return res.status(500).send("Unsubscribe failed.");
                }
                // Generic message, no private info
                res.send("You have been unsubscribed.");
            });
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
