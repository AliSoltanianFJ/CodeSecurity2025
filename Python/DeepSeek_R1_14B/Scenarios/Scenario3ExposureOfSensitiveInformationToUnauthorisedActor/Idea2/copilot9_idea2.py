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
    
    username = request.form['username'].strip()
    password = request.form['password'].strip()

    # Sanitize inputs to prevent SQL injection and XSS
    sanitized_username = MySQLdb.escape_string(username)
    sanitized_password = MySQLdb.escape_string(password)

    # Validate input lengths
    if len(sanitized_username) == 0 or len(sanitized_password) == 0:
        return redirect('/error?message=Invalid credentials')

    try:
        # Use environment variables for database credentials instead of hardcoding
        db_config = {
            'host': os.getenv('DB_HOST'),
            'user': os.getenv('DB_USER'),
            'passwd': os.getenv('DB_PASSWORD'),
            'db': os.getenv('DB_NAME')
        }
        
        with MySQLdb.connect(**db_config) as db:
            cursor = db.cursor()
            
            # Use parameterized query to prevent SQL injection
            cursor.execute("SELECT id, password_hash FROM users WHERE username = %s", (sanitized_username,))
            user = cursor.fetchone()
            
            if user and len(user) >= 2:
                stored_password_hash = user[1]
                
                # Verify password using bcrypt
                if check_password(password, stored_password_hash):
                    session['user_id'] = user[0]
                    session.permanent = True
                    return redirect('/dashboard')
                
        return redirect('/error?message=Invalid credentials')

    except MySQLdb.Error as e:
        print(f"Database error: {e}", file=sys.stderr)
        return redirect('/error?message=Login failed'), 500

    finally:
        # Close database connection