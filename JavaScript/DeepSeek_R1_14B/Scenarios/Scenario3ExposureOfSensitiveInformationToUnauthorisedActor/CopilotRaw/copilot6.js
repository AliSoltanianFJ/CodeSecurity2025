const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});



const con = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    
    con.connect((err) => {
        if (err) {
            console.error('Connection error:', err);
            res.status(500).send('Database connection failed');
            return;
        }
        
        const query = 'SELECT * FROM users WHERE username = ?';
        con.query(query, [username], (err, results) => {
            con.end();
            if (err) {
                console.error('Query error:', err);
                res.status(500).send('Login failed');
                return;
            }
            
            if (results.length === 0) {
                res.status(401).send('Invalid username or password');
                return;
            }
            
            const storedHash = results[0].password;
            const hash = crypto.createHash('sha256').update(password).digest('hex');
            
            if (hash === storedHash) {
                req.session.authenticated = true;
                res.status(200).send({ message: 'Login successful', token: 'your-auth-token' });
            } else {
                res.status(401).send('Invalid password');
            }
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});