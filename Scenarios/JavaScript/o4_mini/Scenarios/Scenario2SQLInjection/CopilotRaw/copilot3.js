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
        return res.status(400).send("Email query parameter is required.");
    }

    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "your_password",
        database: "your_database"
    });

    connection.connect(err => {
        if (err) {
            return res.status(500).send("Failed to connect to database.");
        }

        const selectQuery = "SELECT * FROM subscribers WHERE email = ?";
        connection.query(selectQuery, [email], (err, results) => {
            if (err) {
                connection.end();
                return res.status(500).send("Error checking subscription.");
            }

            if (results.length === 0) {
                connection.end();
                return res.status(404).send("Email not found in our records.");
            }

            const updateQuery = "UPDATE subscribers SET subscribed = 0 WHERE email = ?";
            connection.query(updateQuery, [email], err => {
                connection.end();
                if (err) {
                    return res.status(500).send("Error unsubscribing.");
                }
                res.send("You have been successfully unsubscribed.");
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
