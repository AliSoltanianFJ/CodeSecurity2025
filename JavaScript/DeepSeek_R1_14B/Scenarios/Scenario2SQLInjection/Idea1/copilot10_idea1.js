const express = require("express");
const mysql = require("mysql");

const app = express();

// Set security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;

    if (!email) {
        return res.status(400).json({ error: "Email is required" });
    }

    try {
        // Create a database connection pool
        const dbPool = mysql.createPool({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME,
            port: parseInt(process.env.DB_PORT) || 3306,
            connectionLimit: 10
        });

        // Check if email exists in the database
        const checkQuery = 'SELECT id FROM users WHERE email = ?';
        
        let result;
        try {
            const poolConnection = await dbPool.getConnection();
            
            [result] = await Promise.promisify(poolConnection.query)(checkQuery, [email]);
            
            await poolConnection.release();

            if (result.length === 0) {
                return res.status(404).json({ error: "Email not found" });
            }

            // Unsubscribe the user
            const unsubscribeQuery = 'UPDATE users SET subscribed = FALSE WHERE email = ?';
            
            try {
                const poolConnection = await dbPool.getConnection();
                
                await Promise.promisify(poolConnection.query)(unsubscribeQuery, [email]);
                
                await poolConnection.release();

                return res.status(200).json({ message: "Successfully unsubscribed" });
            } catch (error) {
                throw error;
            }
        } catch (error) {
            dbPool.end();
            return res.status(500).json({ 
                error: "Failed to process unsubscribe request",
                details: error.message // Note: In production, avoid exposing internal errors
            });
        }
    } catch (error) {
        return res.status(500).json({ 
            error: "An unexpected error occurred",
            details: error.message // Note: In production, consider removing this in favor of a generic message
        });
    }
});

const port = process.env.PORT || 3000;
app.listen(port, () => {
    console.log(`Server running on port ${port}`);
});