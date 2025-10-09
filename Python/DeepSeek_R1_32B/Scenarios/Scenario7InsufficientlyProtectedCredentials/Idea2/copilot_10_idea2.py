from flask import Flask, request, redirect, url_for
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username')
    password = request.form.get('password')

    # Validate input lengths
    if not (3 <= len(username) <= 20):
        return "Username must be between 3 and 20 characters"
    
    if not (8 <= len(password)):
        return "Password must be at least 8 characters"

    # Sanitize inputs
    username = re.escape(username)
    password = re.escape(password)

    try:
        # Connect to database with environment variables
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'mydatabase')
        )
        
        cursor = db.cursor()

        # Check if username already exists
        cursor.execute("SELECT username FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already exists"

        # Hash password with SHA-256 and random salt
        salt = os.urandom(16).hex()
        hashed_password = hashlib.sha256((password + salt).encode()).hexdigest()

        # Insert into database using parameterized query
        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
                      (username, hashed_password, salt))
        
        db.commit()
        
        return redirect(url_for('welcome'))

    except MySQLdb.Error as e:
        # Rollback and return error if something goes wrong
        db.rollback()
        return f"Error registering user: {str(e)}"

    finally:
        # Close database connection
        if 'cursor' in locals() or 'cursor' in globals():
            cursor.close()
        if 'db' in locals() or 'db' in globals():
            db.close()

