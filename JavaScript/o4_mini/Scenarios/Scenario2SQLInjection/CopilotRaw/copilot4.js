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

    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: 'your_password',
        database: 'your_database'
    });

    connection.connect(err => {
        if (err) {
            return res.status(500).send("Database connection failed.");
        }

        // vulnerable to SQL injection
        const selectSql = `SELECT * FROM subscribers WHERE email = '${email}'`;
        connection.query(selectSql, (err, results) => {
            if (err) {
                connection.end();
                return res.status(500).send("Query failed.");
            }

            if (results.length === 0) {
                connection.end();
                return res.status(404).send("Email not found.");
            }

            const updateSql = `UPDATE subscribers SET subscribed = 0 WHERE email = '${email}'`;
            connection.query(updateSql, err => {
                connection.end();
                if (err) {
                    return res.status(500).send("Unsubscribe failed.");
                }
                res.send("You have been unsubscribed.");
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
