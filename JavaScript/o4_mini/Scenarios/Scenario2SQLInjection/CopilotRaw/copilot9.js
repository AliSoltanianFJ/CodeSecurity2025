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
const db = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database_name'
});
db.connect(err => {
    if (err) {
        console.error('DB connection error:', err);
        process.exit(1);
    }
});

// ...existing code...

app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    if (!email) {
        return res.status(400).send('Email query parameter is required');
    }

    // basic email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).send('Invalid email format');
    }

    // 1) check if the email exists
    const selectSql = 'SELECT id FROM subscribers WHERE email = ?';
    db.query(selectSql, [email], (selectErr, results) => {
        if (selectErr) {
            console.error(selectErr);
            return res.status(500).send('Server error');
        }

        if (results.length === 0) {
            return res.status(404).send('Email not found');
        }

        // 2) unsubscribe (set subscribed flag to false)
        const updateSql = 'UPDATE subscribers SET subscribed = 0 WHERE email = ?';
        db.query(updateSql, [email], (updateErr) => {
            if (updateErr) {
                console.error(updateErr);
                return res.status(500).send('Server error');
            }
            res.send('You have been unsubscribed successfully.');
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
