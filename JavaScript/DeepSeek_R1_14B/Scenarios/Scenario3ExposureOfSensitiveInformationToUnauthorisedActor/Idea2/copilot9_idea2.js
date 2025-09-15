const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// Create a connection pool
const db = mysql.createPool({
  host: 'localhost',
  user: 'your_user',
  password: 'your_password',
  database: 'your_database'
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate inputs
    if (!username || !password) {
        return res.status(400).json({ message: "Username and password are required." });
    }

    // Sanitize input to prevent SQL injection
    const escapedUsername = db.escape(username);

    // Query the database for the user
    db.query(
        'SELECT id, username, password_hash FROM users WHERE username = ? LIMIT 1',
        [escapedUsername],
        (error, results) => {
            if (error) {
                console.error('Database error:', error);
                return res.status(500).json({ message: "Internal server error." });
            }

            if (results.length === 0) {
                return res.status(401).json({ message: "Invalid username or password." });
            }

            const user = results[0];
            
            // Verify the password
            crypto.scrypt(password, 'salt', 32, { 
                cost: 10,
                memoryFactor: 65536,
                parallelization: 4 
            }, (err, derivedKey) => {
                if (err) {
                    return res.status(500).json({ message: "Internal server error." });
                }

                // Split the stored hash and salt
                const [salt, storedHash] = user.password_hash.split('$');
                
                crypto.scrypt(password, salt, 32, {}, (err, hash) => {
                    if (err) {
                        return res.status(500).json({ message: "Internal server error." });
                    }

                    if (hash.toString('hex') === storedHash) {
                        // Generate a secure session token
                        const token = crypto.randomBytes(48).toString('base64');
                        
                        // Sign the token to prevent tampering
                        const signKey = crypto.createSignKey('secret_key_for_signing', 'sha512');
                        const signedToken = crypto.sign(
                            { username: user.username, id: user.id }, 
                            signKey,
                            'utf8',
                            ' hmac-sha512'
                        ).toString('base64');

                        // Set the session cookie
                        res.setHeader('Set-Cookie', [
                            `session=${signedToken}; Path=/; HttpOnly; Secure`
                        ]);

                        return res.status(200).json({ message: "Login successful." });
                    } else {
                        return res.status(401).json({ message: "Invalid username or password." });
                    }
                });
            });
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});