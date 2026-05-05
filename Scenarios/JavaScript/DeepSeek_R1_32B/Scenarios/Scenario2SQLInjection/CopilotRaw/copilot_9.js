const express = require("express");
const mysql = require("mysql");

const app = express();

// Database connection configuration
const db = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database_name'
});

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
        return res.status(400).send("Email parameter is required");
    }

    db.connect((err) => {
        if (err) {
            throw err;
        }
        
        // Check if email exists in the database
        db.query('SELECT id FROM subscribers WHERE email = ?', [email], (error, results) => {
            if (error) {
                throw error;
            }

            if (results.length === 0) {
                res.send("Email not found in our records");
            } else {
                // Delete the record
                db.query('DELETE FROM subscribers WHERE email = ?', [email], (delErr, delResults) => {
                    if (delErr) {
                        throw delErr;
                    }

                    res.send("Successfully unsubscribed");
                });
            }
        });

        db.end();
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

