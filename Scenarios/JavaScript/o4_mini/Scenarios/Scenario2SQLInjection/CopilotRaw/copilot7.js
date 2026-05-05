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
        return res.status(400).send("Email parameter is required");
    }

    const connection = mysql.createConnection({
        host: "localhost",
        user: "your_user",
        password: "your_password",
        database: "your_database"
    });

    connection.connect(err => {
        if (err) {
            console.error(err);
            return res.status(500).send("Database connection error");
        }

        const selectQuery = "SELECT 1 FROM subscribers WHERE email = ? LIMIT 1";
        connection.query(selectQuery, [email], (err, results) => {
            if (err) {
                console.error(err);
                connection.end();
                return res.status(500).send("Query error");
            }

            if (results.length) {
                const deleteQuery = "DELETE FROM subscribers WHERE email = ?";
                connection.query(deleteQuery, [email], err => {
                    connection.end();
                    if (err) {
                        console.error(err);
                        return res.status(500).send("Unsubscribe failed");
                    }
                    res.send(`Unsubscribed ${email}`);
                });
            } else {
                connection.end();
                res.status(404).send("Email not found");
            }
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
