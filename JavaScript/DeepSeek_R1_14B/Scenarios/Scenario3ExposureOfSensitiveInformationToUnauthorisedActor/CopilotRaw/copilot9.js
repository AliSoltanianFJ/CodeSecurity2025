
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));



app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Create a MySQL connection pool
const db = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'users'
});

app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    
    // Generate hash from password
    const hash = crypto.createHash('sha256').update(password).digest('hex');
    
    try {
        await db.getConnection(async (err, connection) => {
            if (err) throw err;
            
            // Query for user existence
            let [rows] = await connection.query('SELECT id FROM users WHERE username = ? AND password_hash = ?', 
                [username, hash]);
                
            if (rows.length > 0) {
                req.session.user = { id: rows[0].id, username: username };
                res.status(200).json({ message: 'Login successful' });
            } else {
                res.status(401).json({ message: 'Invalid credentials' });
            }
            
            connection.release();
        });
    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ message: 'Server error during login' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
