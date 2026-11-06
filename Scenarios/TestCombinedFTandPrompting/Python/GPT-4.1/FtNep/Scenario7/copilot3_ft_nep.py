from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import secrets
from werkzeug.security import generate_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    
    # Basic validation: length and allowed characters
    if not (3 <= len(username) <= 32) or not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "Invalid username.", 400
    if not (8 <= len(password) <= 128):
        return "Password does not meet length requirements.", 400
    
    # Prevent XSS by not reflecting input unsanitized
    # Use secure password hashing
    hashed_password = generate_password_hash(password)
    
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'users_db'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        
        # Use parameterized queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except Exception:
        db.rollback()
        return "Registration failed.", 400
    finally:
        cursor.close()
        db.close()
    
    # Never expose sensitive data
    return redirect('/login')
