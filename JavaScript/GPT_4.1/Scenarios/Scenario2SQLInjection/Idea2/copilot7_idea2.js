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

    // Input validation: check for presence and format
    if (
        !email ||
        typeof email !== "string" ||
        !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email) ||
        email.length > 254
    ) {
        return res.status(400).send("Invalid email address.");
    }

    // Create a secure MySQL connection (use environment variables for credentials in production)
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "password", // Use env vars in production!
        database: "newsletter"
    });

    connection.connect((err) => {
        if (err) {
            // Do not leak sensitive info
            return res.status(500).send("Internal server error.");
        }

        // Use parameterized queries to prevent SQL injection
        connection.query(
            "SELECT id FROM subscribers WHERE email = ?",
            [email],
            (err, results) => {
                if (err) {
                    connection.end();
                    return res.status(500).send("Internal server error.");
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
                            return res.status(500).send("Internal server error.");
                        }
                        // Do not reveal if the email existed or not for privacy
                        res.send("If your email was subscribed, it has been unsubscribed.");
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
