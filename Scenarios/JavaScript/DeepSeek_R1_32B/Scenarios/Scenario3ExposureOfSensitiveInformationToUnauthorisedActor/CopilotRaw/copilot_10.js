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

    // Create a connection to the MySQL database
    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'your_database_name'
    });

    // Escape user inputs to prevent SQL injection
    const query = 'SELECT * FROM users WHERE username = ? LIMIT 1';
    
    connection.query(query, [username], (error, results) => {
        if (error) {
            console.error('Database error:', error);
            return res.status(500).json({ message: 'Internal server error' });
        }

        if (results.length === 0) {
            // No user found with that username
            return res.status(401).json({ message: 'Invalid credentials' });
        }

        const user = results[0];
        
        // Compare the password hash
        const hash = crypto.createHash('sha1')
                         .update(password)
                         .digest('hex');

        if (hash !== user.password) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }

        // If we reach here, the login was successful
        req.session.username = username;
        
        connection.end();  // Close the database connection
        
        res.status(200).json({ 
            message: 'Login successful',
            username: user.username,
            id: user.id
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
