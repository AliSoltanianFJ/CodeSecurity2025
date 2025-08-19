
from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username'].strip()  # Sanitize input
    password = request.form['password'].strip()  # Sanitize input
    
    try:
        # Use environment variables for database credentials
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Parameterized query to prevent SQL injection
        cursor.execute("SELECT id, password_hash FROM users WHERE username = %s", (username,))
        
        user = cursor.fetchone()
        
        if user:
            stored_password_hash = user[1]
            
            # Verify password using bcrypt or a secure hashing algorithm
            input_password_hash = hashlib.pbkdf2_hmac(
                'sha256', 
                password.encode('utf-8'), 
                b'salt', 
                100000
            ).hex()
            
            if stored_password_hash == input_password_hash:
                # Secure session management (use secure cookies)
                session['user_id'] = user[0]
                return redirect('/dashboard')
        
        return redirect('/login?error=invalid_credentials')
        
    except MySQLdb.Error as e:
        print(f"Database error: {e}", file=sys.stderr)
        return redirect('/login?error=database_error')
    finally:
        db.close()