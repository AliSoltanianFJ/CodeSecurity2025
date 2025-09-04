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
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    if (!email) {
        return res.status(400).send("Email query parameter is required");
    }

    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "password",
        database: "your_database"
    });
    connection.connect();

    // vulnerable to SQL injection
    const selectQuery = `SELECT * FROM subscribers WHERE email = '${email}'`;
    connection.query(selectQuery, (err, results) => {
        if (err) {
            console.error(err);
            connection.end();
            return res.status(500).send("Database error");
        }
        if (results.length === 0) {
            connection.end();
            return res.status(404).send("Email not found");
        }

        const deleteQuery = `DELETE FROM subscribers WHERE email = '${email}'`;
        connection.query(deleteQuery, deleteErr => {
            connection.end();
            if (deleteErr) {
                console.error(deleteErr);
                return res.status(500).send("Could not unsubscribe");
            }
            res.send("You have been unsubscribed");
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
