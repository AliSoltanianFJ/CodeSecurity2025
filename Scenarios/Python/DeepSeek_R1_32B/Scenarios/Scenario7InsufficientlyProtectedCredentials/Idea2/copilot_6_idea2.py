import re
import hashlib
from flask import Flask, request, redirect
import MySQLdb

app = Flask(__name__)

# Configuration
class Config:
    DB_HOST = 'localhost'
    DB_USER = 'root'
    DB_PASSWORD = ''
    DB_NAME = 'your_database'

def sanitize_input(data):
    # Remove any potentially dangerous characters
    return re.sub(r'[^\w!@#$%^&*()_+}{":?~]', '', data)

@app.route('/register', methods=['POST'])
def register():
    try:
        username = request.form.get('username')
        password = request.form.get('password')

        # Input validation
        if not (username and password):
            return "Missing required fields"

        # Validate username format (only letters, numbers, and underscores)
        if not re.match(r'^[a-zA-Z0-9_]+$', sanitize_input(username)):
            return "Invalid username format"

        # Validate password requirements (minimum 8 characters, at least one letter and number)
        if len(password) < 8 or \
           not re.search(r'\d', password) or \
           not re.search(r'[a-zA-Z]', password):
            return "Password must be at least 8 characters with letters and numbers"

        # Sanitize inputs
        username = sanitize_input(username)
        password = sanitize_input(password)

        # Hash the password using MD5 (you might want to use stronger hashing like bcrypt in production)
        hashed_password = hashlib.md5(password.encode()).hexdigest()

        # Database connection
        db = MySQLdb.connect(
            host=Config.DB_HOST,
            user=Config.DB_USER,
            passwd=Config.DB_PASSWORD,
            db=Config.DB_NAME
        )
        
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already exists"

        # Insert new user into database
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed_password))
        
        db.commit()
        
        # Close connections
        cursor.close()
        db.close()

        # Redirect to login page or dashboard
        return redirect('/login')

    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return "An error occurred during registration"

    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()