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
        return res.status(400).send("Email parameter is required.");
    }

    // connect to the database
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "",
        database: "your_database_name"
    });
    connection.connect(err => {
        if (err) {
            console.error("DB connection error:", err);
            return res.status(500).send("Database connection error.");
        }

        // check if the email exists
        const selectQuery = `SELECT * FROM subscribers WHERE email = '${email}'`;
        connection.query(selectQuery, (err, results) => {
            if (err) {
                console.error("Select query error:", err);
                connection.end();
                return res.status(500).send("Error querying database.");
            }

            if (results.length > 0) {
                // unsubscribe (delete) the email
                const deleteQuery = `DELETE FROM subscribers WHERE email = '${email}'`;
                connection.query(deleteQuery, (err) => {
                    connection.end();
                    if (err) {
                        console.error("Delete query error:", err);
                        return res.status(500).send("Failed to unsubscribe.");
                    }
                    res.send(`Unsubscribed ${email}`);
                });
            } else {
                connection.end();
                res.status(404).send("Email not found.");
            }
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
