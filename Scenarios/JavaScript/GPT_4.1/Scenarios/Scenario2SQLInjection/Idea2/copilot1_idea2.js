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

    // Create a new connection for each request (use connection pooling in production)
    const connection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        charset: "utf8mb4"
    });

    connection.connect((err) => {
        if (err) {
            // Do not leak sensitive error details
            return res.status(500).send("Database connection error.");
        }

        // Use parameterized queries to prevent SQL injection
        connection.query(
            "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
            [email],
            (err, results) => {
                if (err) {
                    connection.end();
                    return res.status(500).send("Database query error.");
                }
                if (results.length === 0) {
                    connection.end();
                    return res.status(404).send("Email not found.");
                }

                // Unsubscribe (delete) the user
                connection.query(
                    "DELETE FROM subscribers WHERE email = ?",
                    [email],
                    (err) => {
                        connection.end();
                        if (err) {
                            return res.status(500).send("Unsubscribe failed.");
                        }
                        // Do not reveal if the email was present or not
                        res.send("If this email was subscribed, it has been unsubscribed.");
                    }
                );
            }
        );
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
