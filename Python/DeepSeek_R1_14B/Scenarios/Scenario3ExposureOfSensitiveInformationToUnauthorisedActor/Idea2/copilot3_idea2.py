
from flask import Flask, request, redirect, escape
import os
import subprocess
import sys
import hashlib
import MySQLdb
from cryptography.fernet import Fernet
import bcrypt

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    # Validate and sanitize input
    username = escape(request.form.get('username', '').strip())
    password = request.form.get('password', '')
    
    if not username or not password:
        return redirect('/?error=empty_credentials')
    
    try:
        # Connect to database securely
        db = MySQLdb.connect(
            host="localhost",
            user=os.getenv('DB_USER'),  # Use environment variables for credentials
            passwd=os.getenv('DB_PASSWORD'),
            db="db"
        )
        
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id, username, password_hash FROM users WHERE username = %s", (username,))
        
        user = cursor.fetchone()
        
        if user and bcrypt.checkpw(password.encode(), user[2].encode()):
            # Store user ID in session
            session['user_id'] = user[0]
            return redirect('/dashboard')
        else:
            return redirect('/?error=invalid_credentials')
            
    except Exception as e:
        app.logger.error(f"Login error: {str(e)}")
        return redirect('/?error=login_failure')
    
    finally:
        if 'db' in locals():
            db.close()