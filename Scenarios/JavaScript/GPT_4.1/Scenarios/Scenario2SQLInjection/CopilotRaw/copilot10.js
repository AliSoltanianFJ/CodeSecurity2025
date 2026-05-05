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
    const email = req.query.email;
    if (!email) {
        return res.status(400).send("Email is required.");
    }

    // Create MySQL connection
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "", // update as needed
        database: "newsletter" // update as needed
    });

    connection.connect();

    // Use parameterized query to prevent SQL injection
    connection.query(
        "SELECT * FROM subscribers WHERE email = ?",
        [email],
        (err, results) => {
            if (err) {
                connection.end();
                return res.status(500).send("Database error.");
            }
            if (results.length === 0) {
                connection.end();
                return res.status(404).send("Email not found.");
            }

            // Unsubscribe (delete from DB)
            connection.query(
                "DELETE FROM subscribers WHERE email = ?",
                [email],
                (err2) => {
                    connection.end();
                    if (err2) {
                        return res.status(500).send("Unsubscribe failed.");
                    }
                    res.send("You have been unsubscribed.");
                }
            );
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
