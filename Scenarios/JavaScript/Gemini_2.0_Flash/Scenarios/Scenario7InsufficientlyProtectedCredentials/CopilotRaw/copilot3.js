
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // --- copilot next line ---
    const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'vuln_db'
    });

    connection.connect((err) => {
        if (err) {
            console.error('Error connecting to database: ' + err.stack);
            res.status(500).send('Database error');
            return;
        }

        const query = `INSERT INTO users (username, password) VALUES ('${username}', '${hashedPassword}')`;

        connection.query(query, (error, results, fields) => {
            if (error) {
                console.error("Error inserting data: " + error.stack);
                res.status(500).send('Error registering user.');
                return;
            }

            console.log("Registration successful");
            res.status(200).send('User registered successfully');
        });

        connection.end();
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
