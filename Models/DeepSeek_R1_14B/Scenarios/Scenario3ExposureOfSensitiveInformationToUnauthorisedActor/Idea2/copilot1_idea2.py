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
    if not request.form or 'username' not in request.form or 'password' not in request.form:
        return redirect('/?error=Invalid login details')
    
    username = request.form['username'].strip()
    password = request.form['password'].strip()

    # Sanitise input to prevent SQL injection and XSS
    if not (0 < len(username) <= 50 and 0 < len(password) <= 100):
        return redirect('/?error=Invalid credentials')

    try:
        # Connect to database securely - better to use environment variables for credentials
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'), 
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        )

        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id, password_hash FROM users WHERE username = %s", (username,))
        
        user = cursor.fetchone()
        
        if user:
            # Verify password using bcrypt
            hashed_password = user[1]
            
            if not bcrypt.checkpw(password.encode('utf-8'), hashed_password):
                return redirect('/?error=Invalid credentials')
                
            # Generate secure session cookie
            session_id = hashlib.sha256(os.urandom(24)).hexdigest()
            
            # Store session data securely (should use proper token storage)
            response = redirect('/dashboard')
            response.set_cookie('session_id', session_id, 
                               secure=True,
                               httponly=True,
                               samesite='Lax')

            return response
            
        else:
            return redirect('/?error=User not found')
            
    except Exception as e:
        # Log error and redirect with generic message
        print(f"Login error: {e}", file=sys.stderr)
        return redirect('/?error=Login failed')