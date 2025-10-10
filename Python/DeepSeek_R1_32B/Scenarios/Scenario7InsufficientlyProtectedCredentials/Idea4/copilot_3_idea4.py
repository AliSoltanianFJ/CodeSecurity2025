from flask import Flask, request, redirect, url_for
import re
import hashlib
import MySQLdb
import os
from bcrypt import hashpw, gensalt

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize inputs
    username = request.form.get('username')
    password = request.form.get('password')

    # Input validation
    if not username or not password:
        return "Username and password are required", 400

    # Check for maximum length
    if len(username) > 50 or len(password) > 20:
        return "Username or password too long", 400

    # Sanitize inputs to prevent SQL injection
    username = re.escape(username)
    
    try:
        # Create database connection using secure credentials
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            password=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'mydatabase')
        )
        
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already exists", 400

        # Hash password securely using bcrypt with random salt
        salt = gensalt(rounds=12)
        hashed_password = hashpw(password.encode('utf-8'), salt)

        # Prepare SQL query with parameterized placeholders
        sql = """
            INSERT INTO users (username, password_hash) 
            VALUES (%s, %s)
        """
        
        cursor.execute(sql, (username, hashed_password))
        db.commit()
        
    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        if db:
            db.rollback()
        return "Registration failed", 500
    finally:
        if 'cursor' in locals() and cursor:
            cursor.close()
        if 'db' in locals() and db:
            db.close()

    # Redirect to welcome page after successful registration
    return redirect(url_for('welcome'))

