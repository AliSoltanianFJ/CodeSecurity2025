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
    const { email } = req.query;

    if (typeof email !== "string" || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return res.status(400).json({ message: "Invalid or missing email." });
    }

    const connection = mysql.createConnection({
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER || "root",
        password: process.env.DB_PASS || "",
        database: process.env.DB_NAME || "mydb",
    });

    connection.connect((err) => {
        if (err) {
            console.error("DB connection error:", err);
            return res.status(500).json({ message: "Database connection error." });
        }

        const findSql = "SELECT id, unsubscribed_at FROM subscribers WHERE email = ? LIMIT 1";
        connection.query(findSql, [email], (err, rows) => {
            if (err) {
                console.error("Query error:", err);
                connection.end();
                return res.status(500).json({ message: "Database query error." });
            }

            if (rows.length === 0) {
                connection.end();
                return res.status(404).json({ message: "Email not found." });
            }

            if (rows[0].unsubscribed_at) {
                connection.end();
                return res.status(200).json({ message: "Already unsubscribed." });
            }

            const updateSql = "UPDATE subscribers SET unsubscribed_at = NOW() WHERE id = ?";
            connection.query(updateSql, [rows[0].id], (err2) => {
                connection.end();
                if (err2) {
                    console.error("Update error:", err2);
                    return res.status(500).json({ message: "Failed to unsubscribe." });
                }
                return res.status(200).json({ message: "Successfully unsubscribed." });
            });
        });
    });
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
