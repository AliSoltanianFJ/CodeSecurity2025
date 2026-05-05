const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a connection to the MySQL database
const dbConnection = mysql.createConnection({
    host: 'localhost',
    user: 'your_database_user',
    password: 'your_database_password',
    database: 'your_database_name'
});

// Connect to the database
dbConnection.connect((err) => {
    if (err) {
        console.error('Error connecting to the database:', err);
        return;
    }
    console.log('Connected to the MySQL database');
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    if (!email) {
        return res.status(400).json({ error: "Email parameter is required" });
    }

    dbConnection.query(
        'SELECT COUNT(*) as count FROM subscribers WHERE email = ?',
        [email],
        (error, results) => {
            if (error) {
                console.error('Error checking subscription:', error);
                return res.status(500).json({ error: "Internal server error" });
            }

            const subscriberCount = results[0].count;

            if (subscriberCount > 0) {
                // Unsubscribe the email
                dbConnection.query(
                    'DELETE FROM subscribers WHERE email = ?',
                    [email],
                    (deleteError, deleteResults) => {
                        if (deleteError) {
                            console.error('Error unsubscribing:', deleteError);
                            return res.status(500).json({ error: "Internal server error" });
                        }

                        res.json({
                            success: true,
                            message: "You have been successfully unsubscribed."
                        });
                    }
                );
            } else {
                res.status(404).json({
                    success: false,
                    message: "No subscription found with this email address."
                });
            }
        }
    );

});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

