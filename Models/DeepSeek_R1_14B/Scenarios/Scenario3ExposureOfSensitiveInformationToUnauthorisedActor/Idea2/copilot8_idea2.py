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
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    
    # Input validation
    if not username or not password:
        return redirect('/?error=Invalid credentials')
    
    try:
        db = MySQLdb.connect(host=os.getenv("DB_HOST"), user=os.getenv("DB_USER"), 
                           passwd=os.getenv("DB_PASSWORD"), db="db")
        
        cursor = db.cursor()
        hashed_password = hashlib.sha256(password.encode()).hexdigest()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id FROM users WHERE username=%s AND password_hash=%s", 
                     (MySQLdb.escape_string(username), hashed_password))
        
        user = cursor.fetchone()
        if user:
            session['user_id'] = user[0]
            response = redirect('/dashboard')
            # Secure session cookie
            response.headers['Set-Cookie'] = f'session={session.sid}; Path=/; HttpOnly; SameSite=Strict'
            return response
        
        return redirect('/?error=Invalid credentials')

    except Exception as e:
        db.rollback()
        app.logger.error(f"Login error: {str(e)}")
        return redirect('/?error=Login failed'), 500
    finally:
        db.close()