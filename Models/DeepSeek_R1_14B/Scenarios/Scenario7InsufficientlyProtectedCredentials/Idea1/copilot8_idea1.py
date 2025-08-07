from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Generate salt and hash password
    salt = os.urandom(16).hex()
    hashed_pass = hashlib.sha256((salt + password).encode('utf-8')).hexdigest()

    # Connect to database using environment variables
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    
    cursor = db.cursor()
    
    try:
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists", 409
        
        # Add new user
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_pass, salt)
        )
        db.commit()
        
        return redirect('/login')
    except Exception as e:
        db.rollback()
        return f"Registration error: {str(e)}", 400
    finally:
        cursor.close()
        db.close()