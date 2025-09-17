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

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Sanitize input to prevent XSS
    const sanitizedUsername = mysql.escape(username);
    
    // Database connection configuration
    const dbConfig = {
        host: 'localhost',
        user: 'root',
        database: 'your_database'
    };

    // Create a secure password hashing function
    async function hashPassword(password) {
        const salt = crypto.randomBytes(16).toString('utf8');
        return await new Promise((resolve, reject) => {
            crypto.scrypt(password, salt, 32, { 
                cost: 12,
                parallelization: 1 
            }, (err, hash) => {
                if (err) reject(err);
                resolve(salt + ':' + hash.toString('hex'));
            });
        });
    }

    // Check if username already exists
    const connection = mysql.createConnection(dbConfig);
    
    let query = 'SELECT id FROM users WHERE username = ?';
    let values = [sanitizedUsername];
    
    connection.query(query, values, (err, results) => {
        connection.end(); // Close connection after use
        
        if (err) {
            return res.status(500).json({ error: "Database error" });
        }
        
        if (results.length > 0) {
            return res.status(409).json({ error: "Username already exists" });
        }

        // Hash the password
        hashPassword(password)
            .then(hashedPassword => {
                query = 'INSERT INTO users SET username = ?, password_hash = ?';
                values = [sanitizedUsername, hashedPassword];
                
                connection.query(query, values, (err, result) => {
                    connection.end();
                    
                    if (err) {
                        return res.status(500).json({ error: "Registration failed" });
                    }
                    
                    res.status(201).json({ message: "User registered successfully" });
                });
            })
            .catch(error => {
                res.status(500).json({ error: "Password hashing failed" });
            });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
