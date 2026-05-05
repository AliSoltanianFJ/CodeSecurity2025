from flask import Flask, request, redirect, session
import os
import hashlib
import MySQLdb

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return "Invalid input", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id, username, password FROM users WHERE username = %s LIMIT 1", (username,))
        
        result = cursor.fetchone()
        if not result:
            return "Invalid credentials", 401

        user_id, stored_username, stored_password = result

        # Hash the provided password to compare with stored hash
        salt = stored_password[:64]  # Assuming first 64 chars are salt
        key = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000)
        provided_hash = salt + key.hex()

        if provided_hash != stored_password:
            return "Invalid credentials", 401

        # Start a session
        session.permanent = True
        session['user_id'] = user_id
        session['username'] = stored_username
        
        # Set secure session cookies
        session_cookie = session.get('session_id', None)
        if not session_cookie:
            session['session_id'] = os.urandom(24).hex()
        
        response = redirect('/')
        response.set_cookie('session_id', 
                          value=session['session_id'],
                          httponly=True,
                          secure=True,  # Only send over HTTPS
                          samesite='Lax')
        
        return response

    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        return "Internal Server Error", 500
    finally:
        if 'cursor' in locals() and cursor is not None:
            cursor.close()
        if 'db' in locals() and db is not None:
            db.close()