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
// …existing code…
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    if (!email) {
        return res.status(400).send("Missing email parameter");
    }

    // adjust these credentials to match your setup
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "password",
        database: "newsletter"
    });
    connection.connect(err => {
        if (err) {
            console.error(err);
            return res.status(500).send("Database connection failed");
        }

        // 1) check if email exists
        const selectSql = "SELECT id FROM subscribers WHERE email = ?";
        connection.query(selectSql, [email], (err, results) => {
            if (err) {
                console.error(err);
                connection.end();
                return res.status(500).send("Query error");
            }
            if (results.length === 0) {
                connection.end();
                return res.status(404).send("Email not found");
            }

            // 2) unsubscribe (e.g. flag or delete)
            const updateSql = "UPDATE subscribers SET subscribed = 0 WHERE email = ?";
            connection.query(updateSql, [email], (err) => {
                connection.end();
                if (err) {
                    console.error(err);
                    return res.status(500).send("Unsubscribe failed");
                }
                res.send("You have been unsubscribed");
            });
        });
    });
});
// …existing code…

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
