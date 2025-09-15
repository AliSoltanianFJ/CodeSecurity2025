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
    // --- copilot next line ---

    const con = mysql.createConnection({
        host: 'localhost',
        user: 'yourdbuser',
        password: 'yourdbpassword',
        database: 'yourdbname'
    });

    try {
        const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');
        
        con.connect((err) => {
            if (err) throw err;
            
            con.query('SELECT * FROM users WHERE username = ?', [username], (err, result) => {
                if (err) throw err;
                
                if (result.length > 0 && result[0].password === hashedPassword) {
                    res.send({ success: true });
                } else {
                    res.send({ success: false, message: 'Invalid credentials' });
                }
                
                con.end();
            });
        });
    } catch (err) {
        res.status(500).send({ error: err.message });
    }

});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});