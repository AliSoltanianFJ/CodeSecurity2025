
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

    connection.connect((err) => {
        if (err) {
            console.error('Database connection failed: ' + err.stack);
            return res.status(500).send("Database connection error.");
        }

    // Use parameterized query to prevent SQL injection
    const query = "SELECT * FROM subscribers WHERE email = ?";
    connection.query(query, [email], (err, results) => {
        if (err) {
            console.error("Database query failed: " + err.stack);
            connection.end();
            return res.status(500).send("Database query error.");
        }

        if (results.length === 0) {
            connection.end();
            return res.status(404).send("Email not found.");
        }

        // Unsubscribe (delete from database)
        const deleteQuery = "DELETE FROM subscribers WHERE email = ?";
        connection.query(deleteQuery, [email], (err) => {
            connection.end();
            if (err) {
                console.error("Database deletion failed: " + err.stack);
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
