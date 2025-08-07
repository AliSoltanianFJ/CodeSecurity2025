from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    if not request.form['username'] or not request.form['password']:
        return "Username or password cannot be empty"
    
    username = re.escape(request.form['username'].strip())
    password = request.form['password']
    
    # Validate password policy
    if len(password) < 8:
        return "Password must be at least 8 characters long"
    if not re.search(r'[A-Za-z]', password):
        return "Password must contain letters"
    if not re.search(r'\d', password):
        return "Password must contain numbers"
    
    # Generate salt
    salt = os.urandom(16)
    hashed_password = hashlib.bcrypt.hashpw(password.encode('utf-8'), salt)
    
    try:
        # Connect to database (assuming MySQLdb is properly configured)
        db = MySQLdb.connect(...)
        cursor = db.cursor()
        
        # Prepare SQL statement with proper escaping
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password.decode('utf-8'))
        )
        
        db.commit()
        return redirect('/success')
    except Exception as e:
        return f"Registration failed: {str(e)}", 500