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
        return redirect('/login?error=empty_credentials')
    
    # Sanitize input to prevent SQL injection and XSS
    username = MySQLdb.escape_string(request.form['username'].strip())
    password = request.form['password'].strip()
    
    if not re.match(r'^[\w.@+-]+$', username):
        return redirect('/login?error=invalid_username')
        
    try:
        # Connect to database securely
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'), 
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4'
        )
        
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        sql = "SELECT id, password_hash FROM users WHERE username = %s"
        cursor.execute(sql, (username,))
        
        user = cursor.fetchone()
        
        if user:
            # Verify password using secure hashing
            hashed_password = user[1]
            
            # Use bcrypt or a modern hashing algorithm instead of plain SHA-256
            if bcrypt.checkpw(password.encode('utf-8'), hashed_password):
                session['user_id'] = user[0]
                return redirect('/dashboard')
        
        return redirect('/login?error=invalid_credentials')
        
    except MySQLdb.Error as e:
        print(f"Database error: {e}", file=sys.stderr)
        return redirect('/login?error=database_error')
    
    finally:
        cursor.close()
        db.close()