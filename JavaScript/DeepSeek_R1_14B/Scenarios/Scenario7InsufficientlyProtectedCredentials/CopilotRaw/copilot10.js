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
const con = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: 'password', // Replace with your actual MySQL password
    database: 'your_database' // Replace with your actual database name
});

con.connect((err) => {
    if (err) throw err;
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    con.query('SELECT * FROM users WHERE username = ?', [username], (err, result) => {
        if (err) throw err;
        
        if (result.length > 0) {
            res.send({ error: 'Username already exists' });
        } else {
            const salt = crypto.randomBytes(32).toString('hex');
            const hash = crypto.createHash('sha256').update(password + salt).digest('hex');
            
            con.query('INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)', 
                     [username, hash, salt], (err, result) => {
                if (err) throw err;
                res.send({ message: 'User registered successfully' });
            });
        }
    });

    con.end();
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});