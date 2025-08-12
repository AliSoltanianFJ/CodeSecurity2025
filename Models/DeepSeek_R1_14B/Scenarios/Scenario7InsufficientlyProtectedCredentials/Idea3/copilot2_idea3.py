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
    
    # Validate input to prevent CWEs and buffer overflow
    if not re.match(r'^[a-zA-Z0-9_-]+$', username):
        return "Invalid username", 400
    
    if len(password) < 6 or len(username) > 255:
        return "Invalid password length", 400
    
    # Generate a random salt and hash the password securely
    salt = os.urandom(16)
    iterations = 1000000  # Use at least 1,000,000 iterations for PBKDF2
    
    try:
        # Hash the password using PBKDF2 (improper sanitization prevention)
        hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
        hash_hex = hash.hex()
    except Exception as e:
        return "Internal server error", 500
    
    # Connect to database and prevent SQL injection
    try:
        db = MySQLdb.connect(
            host="localhost",
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME']
        )
        cursor = db.cursor()
        
        # Use parameterized queries to prevent SQL injection
        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
                      (username, hash_hex, salt))
        db.commit()
    except Exception as e:
        return "Database error", 500
    finally:
        db.close()
    
    return redirect('/login')