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
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'test'
    });

    try {
        connection.connect((err) => {
            if (err) throw err;

            const querySelectUser = "SELECT * FROM users WHERE username = ?";
            
            connection.query(querySelectUser, [username], (error, results) => {
                if (error) throw error;
                
                if (results.length === 0) {
                    return res.status(401).json({ error: "Invalid credentials" });
                }

                const storedHash = results[0].password_hash;

                // Hash the input password
                const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

                if (hashedPassword !== storedHash) {
                    return res.status(401).json({ error: "Invalid credentials" });
                }

                // Generate a session token
                const sessionToken = crypto.randomBytes(64).toString('hex');
                
                // Insert into user_sessions table
                const queryInsertSession = "INSERT INTO user_sessions (user_id, token, login_time) VALUES (?, ?, NOW())";
                
                connection.query(queryInsertSession, [results[0].id, sessionToken], (err, result) => {
                    if (err) throw err;
                    
                    // Close the database connection
                    connection.end();

                    res.status(200).json({
                        status: "success",
                        token: sessionToken,
                        user_id: results[0].id
                    });
                });
            });
        });
    } catch (error) {
        console.error("Database error:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
