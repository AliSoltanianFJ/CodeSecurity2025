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
    
    crypto.createHash('sha256').update(password).digest('hex');
    
    con.connect((err) => {
        if (err) throw err;
        
        con.query(`SELECT * FROM users WHERE username = ?`, [username], (err, result) => {
            if (err) throw err;
            
            if (result.length > 0) {
                const storedHash = result[0].password;
                
                crypto.createHash('sha256').update(password).digest('hex') === storedHash ?
                    res.status(200).json({ success: true, message: "Login successful!" }) :
                    res.status(401).json({ success: false, message: "Invalid credentials" });
            } else {
                res.status(404).json({ success: false, message: "User not found" });
            }
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
