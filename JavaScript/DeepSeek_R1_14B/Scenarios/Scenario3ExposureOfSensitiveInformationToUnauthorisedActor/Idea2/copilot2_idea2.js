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
    // Validate input
    if (!req.body.username || !req.body.password) {
        return res.status(401).json({ error: "Username and password are required" });
    }

    const username = req.body.username;
    const password = req.body.password;

    // Prevent buffer overflow by validating string lengths
    if (username.length > 255 || password.length > 255) {
        return res.status(401).json({ error: "Invalid input" });
    }

    // Create a secure database connection pool
    const pool = mysql.createPool({
        host: 'localhost',
        user: 'your_user',
        password: 'your_password',
        database: 'your_database'
    });

    // Use prepared statements to prevent SQL injection
    pool.query(
        'SELECT * FROM users WHERE username = ?',
        [username],
        (error, results) => {
            if (error) {
                console.error('Database error:', error);
                return res.status(500).json({ error: "Login failed" });
            }

            const user = results[0];
            if (!user) {
                return res.status(401).json({ error: "Invalid username or password" });
            }

            // Validate password securely using bcrypt
            const hashedPassword = user.password;
            
            // Use scrypt to derive key from password
            crypto.scrypt(password, 'salt', 64, { iterations: 10000 }, (err, derivedKey) => {
                if (err) {
                    return res.status(500).json({ error: "Invalid credentials" });
                }

                const isPasswordValid = crypto.createHash('sha256')
                    .update(derivedKey.toString('hex'))
                    .digest('hex') === hashedPassword;

                if (!isPasswordValid) {
                    return res.status(401).json({ error: "Invalid password" });
                }

                // Set secure session cookie
                const sessionId = crypto.randomUUID();
                
                // Store session data securely in database or use JWT

                res.setHeader('Set-Cookie', [
                    'session-id=' + sessionId + '; Path=/; Secure; HttpOnly'
                ]);
                
                res.status(200).json({ message: "Login successful" });
            });
        }
    );

    pool.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
