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
        return redirect('/?error=Invalid credentials')
    
    username = request.form['username'].strip()
    password = request.form['password'].strip()

    # Sanitize inputs to prevent SQL injection and XSS
    sanitized_username = MySQLdb.escape_string(username)
    sanitized_password = MySQLdb.escape_string(password)

    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),  # Use environment variables for credentials
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        )
        
        cursor = db.cursor()
        hashed_password = hashlib.sha256(password.encode()).hexdigest()  # Store passwords as hashes
        
        query = "SELECT id FROM users WHERE username = %s AND password_hash = %s"
        cursor.execute(query, (sanitized_username, sanitized_password))
        
        if cursor.rowcount == 1:
            session['user_id'] = cursor.fetchone()[0]
            return redirect('/dashboard')
        else:
            return redirect('/?error=Invalid credentials'), 401
            
    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        return redirect('/?error=Server error'), 500
    finally:
        db.close()