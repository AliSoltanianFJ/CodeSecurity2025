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
        return res.send("Email is required");
    }

    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "password",
        database: "users"
    });

    connection.connect((err) => {
        if (err) {
            console.log(err);
            return res.send("Error connecting to database");
        }

        const query = `SELECT * FROM users WHERE email = '${email}'`;

        connection.query(query, (err, results) => {
            if (err) {
                console.log(err);
                return res.send("Error querying database");
            }

            if (results.length > 0) {
                const query = `DELETE FROM users WHERE email = '${email}'`;

                connection.query(query, (err, results) => {
                    if (err) {
                        console.log(err);
                        return res.send("Error deleting from database");
                    }

                    return res.send("Unsubscribed successfully");
                });
            } else {
                return res.send("Email not found");
            }
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
