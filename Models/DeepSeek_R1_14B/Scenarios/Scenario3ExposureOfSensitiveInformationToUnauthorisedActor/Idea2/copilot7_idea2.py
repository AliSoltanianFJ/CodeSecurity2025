
from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)
app.secret_key = os.urandom(24)  # Use a secure random key for sessions

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '', strip=True)
    password = request.form.get('password', '', strip=True)
    
    # Validate input
    if not username or not password:
        return redirect('/login?error=empty_credentials')
    
    try:
        db = MySQLdb.connect(host=os.getenv("DB_HOST"), 
                             user=os.getenv("DB_USER"),
                             passwd=os.getenv("DB_PASSWORD"),
                             db=os.getenv("DB_NAME"))
        
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id, password_hash, role FROM users WHERE username = %s", (username,))
        
        user = cursor.fetchone()
        
        if user and check_password_hash(user[1], password):
            session['logged_in'] = True
            session['user_id'] = str(user[0])
            session['role'] = user[2]
            
            return redirect('/dashboard')
        
        else:
            return redirect('/login?error=invalid_credentials')
            
    except MySQLdb.Error as e:
        print(f"Database error: {e}", file=sys.stderr)
        return redirect('/login?error=database_error')
    
    finally:
        db.close()