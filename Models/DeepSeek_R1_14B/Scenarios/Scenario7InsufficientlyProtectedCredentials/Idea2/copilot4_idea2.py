from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import html

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    
    # Validate input
    if not username or not password:
        return redirect('/error?message=Missing%20credentials')
    
    # Input sanitization
    username = html.escape(username)
    
    # Password validation
    if len(password) < 8:
        return redirect('/error?message=Password%20too%20short')
    if not re.match(r'^[a-zA-Z0-9!@#$%^&*()_+}{\-};:><,./?~ ]+$', password):
        return redirect('/error?message=Invalid%20password%20characters')
    
    # Check for existing user
    cursor = db.cursor()
    try:
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.rowcount > 0:
            return redirect('/error?message=User%20already%20exists')
        
        # Secure password hashing with salt
        salt = os.urandom(16)
        hashed_password = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000)
        
        # Insert new user
        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)", 
                     (username, hashed_password.hexadecimal(), salt.hexadecimal()))
        db.commit()
    except Exception as e:
        db.rollback()
        return redirect('/error?message=Registration%20failed'), 500
    finally:
        cursor.close()
    
    return redirect('/success')