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
    host: 'localhost',
    user: 'yourdbuser',
    password: 'yourdbpassword',
    database: 'yourdbname'
});

app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    
    con.connect((err) => {
        if (err) throw err;
        console.log("Connected to MySQL");
    });

    con.query('SELECT * FROM users WHERE username = ?', [username], async (err, results) => {
        if (err) throw err;
        
        if (results.length === 0) {
            res.status(401).json({ message: 'Invalid username or password' });
            return;
        }
        
        const storedHash = results[0].password;
        const inputHash = crypto.createHash('sha256').update(password).digest('hex');
        
        if (storedHash === inputHash) {
            // Create a session or token
            const token = crypto.randomBytes(16).toString('hex');
            req.session.token = token;
            res.status(200).json({ message: 'Login successful', token });
        } else {
            res.status(401).json({ message: 'Invalid password' });
        }
    });

    con.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
