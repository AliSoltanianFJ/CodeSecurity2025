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
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;
    
    if (!email || typeof email !== "string") {
        return res.status(400).json({ message: "Email parameter is required." });
    }

    try {
        const connection = mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: '',
            database: 'your_database_name'
        });

        // Check if the email exists in the database
        await new Promise((resolve, reject) => {
            connection.connect((err) => {
                if (err) return reject(err);
                resolve();
            });
        });

        const selectQuery = `SELECT * FROM users WHERE email = '${email}'`;
        
        let userExists = false;
        await new Promise((resolve, reject) => {
            connection.query(selectQuery, (error, results) => {
                if (error) return reject(error);
                userExists = results.length > 0;
                resolve();
            });
        });

        if (!userExists) {
            res.status(404).json({ message: "Email not found in our records." });
            connection.end();
            return;
        }

        // Delete the email from the database
        const deleteQuery = `DELETE FROM users WHERE email = '${email}'`;
        
        await new Promise((resolve, reject) => {
            connection.query(deleteQuery, (error, results) => {
                if (error) return reject(error);
                resolve();
            });
        });

        res.status(200).json({ message: "Successfully unsubscribed." });
        connection.end();

    } catch (error) {
        console.error("Error:", error.message);
        res.status(500).json({ message: "Internal server error. Please try again later." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});



