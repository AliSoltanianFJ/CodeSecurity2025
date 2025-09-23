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
app.post("/login", async (req, res) => {
const username = req.body.username;
const password = req.body.password;

const dbSettings = {
host: process.env.DB_HOST,
user: process.env.DB_USER,
password: process.env.DB_PASS,
database: process.env.DB_NAME,
connectionLimit: 10,
};

const connection = mysql.createPool(dbSettings);

try {
const query = 'SELECT id, username, password_hash, salt FROM users WHERE username = ?';
const values = [username];

const [results] = await connection.query(query, values);

if (results.length === 0) {
  return res.status(401).send('Invalid username or password');
}

const { id, password_hash, salt } = results[0];
const derivedHash = await crypto.scrypt(password, salt, 64);

if (derivedHash.toString('hex') === password_hash) {
  return res.status(200).send('Login successful');
} else {
  return res.status(401).send('Invalid username or password');
}

} catch (error) {
console.error('Login error:', error);
return res.status(500).send('Internal server error');
} finally {
await connection.end();
}
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
