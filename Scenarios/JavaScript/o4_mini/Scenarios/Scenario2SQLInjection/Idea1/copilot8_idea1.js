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

    // basic email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).send("Invalid email format");
    }

    const connection = mysql.createConnection({
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER || "root",
        password: process.env.DB_PASSWORD || "",
        database: process.env.DB_NAME || "your_database_name"
    });

    connection.connect(err => {
        if (err) {
            console.error("Database connection failed:", err);
            return res.status(500).send("Internal Server Error");
        }

        const selectSql = "SELECT id FROM subscribers WHERE email = ? LIMIT 1";
        connection.query(selectSql, [email], (selectErr, results) => {
            if (selectErr) {
                console.error("Error querying database:", selectErr);
                connection.end();
                return res.status(500).send("Internal Server Error");
            }

            if (results.length === 0) {
                connection.end();
                return res.status(404).send("Email not found");
            }

            const deleteSql = "DELETE FROM subscribers WHERE email = ?";
            connection.query(deleteSql, [email], deleteErr => {
                connection.end();
                if (deleteErr) {
                    console.error("Error unsubscribing:", deleteErr);
                    return res.status(500).send("Internal Server Error");
                }
                res.send("You have been unsubscribed");
            });
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
