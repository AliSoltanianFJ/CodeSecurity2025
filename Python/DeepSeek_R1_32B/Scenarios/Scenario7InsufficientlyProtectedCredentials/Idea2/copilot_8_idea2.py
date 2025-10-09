
from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    try:
        # Get input values
        username = request.form['username']
        password = request.form['password']

        # Validate inputs
        if not (username and password):
            return "Username and password are required."

        if len(username) > 50 or len(password) < 8:
            return "Invalid username or password length."

        # Sanitize input to prevent SQL injection/XSS
        username = MySQLdb.escape_string(username).decode('utf-8')
        
        # Validate username format (only letters, numbers, and underscores)
        if not re.match(r'^[a-zA-Z0-9_]+$', username):
            return "Invalid username characters."

        # Generate salt and hash password
        salt = os.urandom(16).hex()
        password_hash = hashlib.sha256((password + salt).encode()).hexdigest()

        # Database connection using environment variables for credentials
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'user'),
            passwd=os.getenv('DB_PASSWORD', 'password'),
            db=os.getenv('DB_NAME', 'mydatabase')
        )
        cursor = db.cursor()

        # Check if username already exists
        query = "SELECT COUNT(*) FROM users WHERE username=%s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if result[0] > 0:
            return redirect('/register?error=username_exists')

        # Insert new user into database
        insert_query = """
        INSERT INTO users (username, password_hash, salt) 
        VALUES (%s, %s, %s)
        """
        cursor.execute(insert_query, (username, password_hash, salt))
        db.commit()

        cursor.close()
        db.close()

        # Redirect to success page
        return redirect('/register_success')

    except MySQLdb.Error as e:
        # Log the error for debugging purposes
        print(f"Database error")
        return "An error occurred during registration."

    except Exception as e:
        # Handle other unexpected errors
        print(f"Unexpected error")
        return "An error occurred. Please try again later."

