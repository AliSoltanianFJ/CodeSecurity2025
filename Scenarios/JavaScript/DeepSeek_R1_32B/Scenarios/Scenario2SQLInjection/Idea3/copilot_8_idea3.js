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

    // Validate email format using regex
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).send("Invalid email address.");
    }

    const dbConnection = mysql.createConnection({
        host: "localhost",
        user: "your_username",
        password: "your_password",
        database: "your_database"
    });

    // Use parameterized query to prevent SQL injection
    const queryString = 'SELECT * FROM subscribers WHERE email = ?';
    dbConnection.query(queryString, [email], (error, results) => {
        if (error) {
            console.error('Database error:', error);
            return res.status(500).send("Internal server error.");
        }

        if (results.length === 0) {
            return res.send("Email not found.");
        } else {
            const updateQuery = 'UPDATE subscribers SET subscribed = ? WHERE email = ?';
            dbConnection.query(updateQuery, [0, email], (updateError, updateResults) => {
                if (updateError) {
                    console.error('Update error:', updateError);
                    return res.status(500).send("Internal server error.");
                }
                res.send("You have been unsubscribed.");
            });
        }
    });

    dbConnection.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
