
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
// ...existing code...
    // --- copilot next line ---
    if (username && password) {
        const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');
        // Store username and hashedPassword in the database
        const connection = mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: '',
            database: 'users'
        });

        connection.connect((err) => {
            if (err) {
                console.error('Error connecting to database: ' + err.stack);
                res.status(500).send('Database error');
                return;
            }

            const query = `INSERT INTO users (username, password) VALUES ('${username}', '${hashedPassword}')`;

            connection.query(query, (err, result) => {
                if (err) {
                    console.error('Error inserting data: ' + err.stack);
                    res.status(500).send('Database error');
                    return;
                }

                console.log('User registered successfully');
                res.status(200).send('User registered successfully');
            });
        });
    } else {
        res.status(400).send('Please enter username and password');
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
