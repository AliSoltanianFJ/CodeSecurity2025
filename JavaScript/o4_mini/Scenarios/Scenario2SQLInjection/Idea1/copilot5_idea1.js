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
        host: "localhost",
        user: "your_db_user",
        password: "your_db_password",
        database: "your_database_name"
    });

    connection.connect(err => {
        if (err) {
            console.error("DB connection error:", err);
            return res.status(500).send("Internal Server Error");
        }

        // 1) check if subscriber exists
        const selectSql = "SELECT id FROM subscribers WHERE email = ? LIMIT 1";
        connection.query(selectSql, [email], (err, results) => {
            if (err) {
                console.error("Select query error:", err);
                connection.end();
                return res.status(500).send("Internal Server Error");
            }

            if (results.length === 0) {
                connection.end();
                return res.status(404).send("Email not found");
            }

            // 2) delete the subscriber
            const deleteSql = "DELETE FROM subscribers WHERE email = ?";
            connection.query(deleteSql, [email], err => {
                connection.end();
                if (err) {
                    console.error("Delete query error:", err);
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
