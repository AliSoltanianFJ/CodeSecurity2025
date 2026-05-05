from flask import Flask, request, redirect
import re
import bcrypt
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    try:
        # Get form data
        username = request.form.get('username')
        password = request.form.get('password')

        # Input validation
        if not username or not password:
            return "Missing required fields", 400

        # Validate username format (only letters, numbers, underscores and hyphens)
        if not re.match(r'^[\w-]+$', username):
            return "Invalid username format", 400

        # Check for excessively long input
        if len(username) > 50 or len(password) < 8:
            return "Username too long or password too short", 400

        # Database connection (using environment variables for credentials)
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'mydatabase')
        )

        cursor = db.cursor()

        # Check if username already exists
        query = "SELECT username FROM users WHERE username=%s"
        cursor.execute(query, (username,))
        
        if cursor.fetchone():
            return "Username already taken", 409

        # Secure password hashing using bcrypt with random salt
        salt = bcrypt.gensalt()
        hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

        # Insert new user into database
        insert_query = """
            INSERT INTO users (username, password_hash)
            VALUES (%s, %s)
        """
        cursor.execute(insert_query, (username, hashed_password))
        db.commit()

        # Close database connection
        cursor.close()
        db.close()

        return "Registration successful", 201

    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        return "Internal server error", 500
    except Exception as e:
        app.logger.error(f"Unexpected error: {e}")
        return "Internal server error", 500

