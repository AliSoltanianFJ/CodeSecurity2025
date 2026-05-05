
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
        password: "your_password", // Replace with your actual password
        database: "your_database" // Replace with your actual database name
    });

    connection.connect((err) => {
        if (err) {
            console.error("Error connecting to database: " + err.stack);
            return res.status(500).send("Could not connect to database.");
        }

        // Use parameterized query to prevent SQL injection
        const query = "SELECT * FROM subscribers WHERE email = ?";
        connection.query(query, [email], (err, results) => {
            if (err) {
                console.error("Error executing query: " + err.stack);
                connection.end();
                return res.status(500).send("Database error.");
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
                    console.error("Error executing delete query: " + err.stack);
                    return res.status(500).send("Error unsubscribing.");
                }
                res.send("Successfully unsubscribed.");
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
