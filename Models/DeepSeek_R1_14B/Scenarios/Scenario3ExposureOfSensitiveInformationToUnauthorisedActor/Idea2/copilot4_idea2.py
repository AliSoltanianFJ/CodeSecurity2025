from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Validate input
    if not request.form.get('username') or not request.form.get('password'):
        return redirect('/error?message=Invalid credentials')
    
    # Sanitize inputs
    username = MySQLdb.escape_string(request.form['username'])
    password = request.form['password']
    
    try:
        db = MySQLdb.connect(host=os.getenv("DB_HOST"), user=os.getenv("DB_USER"), 
                           passwd=os.getenv("DB_PASSWORD"), db=os.getenv("DB_NAME"))
        
        cursor = db.cursor()
        hashed_password = hashlib.sha256(password.encode()).hexdigest()  # Use hashing instead of plaintext
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id FROM users WHERE username=%s AND password_hash=%s", 
                     (username, hashed_password))
        
        if cursor.rowcount == 1:
            session['user_id'] = cursor.fetchone()[0]
            return redirect('/dashboard')
        else:
            return redirect('/error?message=Invalid credentials')
    
    except MySQLdb.Error as e:
        print(f"Database error: {e}", file=sys.stderr)
        return redirect('/error?message=Login failed'), 500
    
    finally:
        db.close()