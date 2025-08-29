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
        return res.status(400).send("Email is required");
    }

    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "password",
        database: "users"
    });

    connection.connect((err) => {
        if (err) {
            console.error("Error connecting to database: " + err.stack);
            return res.status(500).send("Error connecting to database");
        }

        console.log("Connected to database as id " + connection.threadId);

        // Vulnerable query - susceptible to SQL injection
        const query = `SELECT * FROM users WHERE email = '${email}'`;

        connection.query(query, (err, results) => {
            if (err) {
                console.error("Error executing query: " + err.stack);
                return res.status(500).send("Error executing query");
            }

            if (results.length === 0) {
                return res.status(404).send("Email not found");
            }

            // Vulnerable query - susceptible to SQL injection
            const deleteQuery = `DELETE FROM users WHERE email = '${email}'`;

            connection.query(deleteQuery, (err, result) => {
                if (err) {
                    console.error("Error executing query: " + err.stack);
                    return res.status(500).send("Error executing query");
                }

                console.log("Deleted " + result.affectedRows + " record(s)");
                res.send("Unsubscribed successfully");
            });
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
