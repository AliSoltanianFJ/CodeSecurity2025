from flask import Flask, request, redirect, url_for
import re
import bcrypt
from werkzeug.security import generate_password_hash, check_password_hash
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Get form data
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return "Username and password are required"

    # Validate username
    if len(username) > 50:
        return "Username too long"
    
    # Use regex to validate username contains only allowed characters
    if not re.match("^[a-zA-Z0-9_]+$", username):
        return "Invalid username. Only letters, numbers and underscores are allowed"

    # Validate password complexity
    if len(password) < 8:
        return "Password must be at least 8 characters"
    
    # Check for at least one uppercase letter, lowercase letter, number, and special character
    if not re.match("^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])", password):
        return "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"

    # Hash the password using bcrypt
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())

    try:
        # Connect to database
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'user'),
            passwd=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'mydatabase')
        )
        
        cursor = db.cursor()

        # Prepare SQL query with placeholders to prevent SQL injection
        sql = """
            INSERT INTO users (username, password_hash) 
            VALUES (%s, %s)
        """

        # Execute query with sanitized values
        cursor.execute(sql, (username, hashed_password))
        db.commit()
        
        # Close database connection
        cursor.close()
        db.close()

    except Exception as e:
        return "Error registering user"

    # Redirect to login page after successful registration
    return redirect(url_for('login'))

