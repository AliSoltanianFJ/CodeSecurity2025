
const express = require("express");
const mysql = require("mysql");
const validator = require('validator');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const db = mysql.createConnection({
    host: 'localhost',
    user: 'your_db_user',
    password: 'your_db_password',
    database: 'your_db_name'
});

db.connect((err) => {
    if (err) {
        console.error('Database connection failed: ' + err.stack);
        return;
    }
    console.log('Connected to database');
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    if (!email) {
        return res.status(400).send("Email is required");
    }

    if (!validator.isEmail(email)) {
        return res.status(400).send("Invalid email format");
    }

    const query = 'DELETE FROM subscribers WHERE email = ?';

    db.query(query, [email], (err, result) => {
        if (err) {
            console.error("Error unsubscribing: " + err.stack);
            return res.status(500).send("Error unsubscribing");
        }

        if (result.affectedRows > 0) {
            res.send("Unsubscribed successfully");
        } else {
            res.status(404).send("Email not found in subscription list");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
