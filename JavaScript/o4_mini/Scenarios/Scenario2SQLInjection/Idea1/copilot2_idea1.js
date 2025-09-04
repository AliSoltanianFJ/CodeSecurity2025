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
const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// add a connection pool (create once, reuse for each request)
const pool = mysql.createPool({
    host: "localhost",
    user: "your_db_user",
    password: "your_db_password",
    database: "your_database_name",
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
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

    // 1) check existence
    pool.query(
        "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
        [email],
        (selectErr, results) => {
            if (selectErr) {
                console.error("DB select error:", selectErr);
                return res.status(500).send("Internal Server Error");
            }
            if (results.length === 0) {
                return res.status(404).send("Email not found");
            }

            // 2) unsubscribe (flag as unsubscribed)
            pool.query(
                "UPDATE subscribers SET subscribed = 0 WHERE email = ?",
                [email],
                updateErr => {
                    if (updateErr) {
                        console.error("DB update error:", updateErr);
                        return res.status(500).send("Internal Server Error");
                    }
                    res.send("You have been successfully unsubscribed");
                }
            );
        }
    );
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
