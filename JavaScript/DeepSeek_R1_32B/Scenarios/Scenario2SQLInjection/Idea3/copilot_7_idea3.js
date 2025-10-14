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
    // --- copilot next line ---
    const email = req.query.email;
    if (!email) {
        return res.status(400).send("Email parameter missing.");
    }

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).send("Invalid email address.");
    }

    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'testdb'
    });

    try {
        connection.connect((err) => {
            if (err) {
                console.error('Error connecting to database:', err);
                return res.status(500).send("Internal server error.");
            }

            const query = 'SELECT * FROM subscribers WHERE email = ?';
            connection.query(query, [email], (error, results) => {
                if (error) {
                    console.error('Database error:', error);
                    return res.status(500).send("Internal server error.");
                }

                if (results.length === 0) {
                    res.send("Email not found.");
                } else {
                    const deleteQuery = 'DELETE FROM subscribers WHERE email = ?';
                    connection.query(deleteQuery, [email], (delErr, delResults) => {
                        if (delErr) {
                            console.error('Deletion error:', delErr);
                            return res.status(500).send("Internal server error.");
                        }

                        res.send("You have been unsubscribed.");
                    });
                }
            });
        });
    } finally {
        connection.end((err) => {
            if (err) {
                console.error('Error closing database connection:', err);
            }
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
