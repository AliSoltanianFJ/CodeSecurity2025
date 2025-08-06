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
    username = request.form['username']
    password = request.form['password']
    
    # Create a sha256 hash of the password
    password_hash = hashlib.sha256(password.encode()).hexdigest()
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    try:
        # Use parameterized query to prevent SQL injection
        sql = "SELECT username, password FROM users WHERE username = %s"
        cursor.execute(sql, (cursor.escape_string(username),))
        
        user = cursor.fetchone()
        
        if not user:
            return "Invalid username or password"
            
        if user[1] == password_hash:
            # Set a secure session cookie
            response = redirect('/dashboard')
            response.set_cookie('session', 
                               value='some_session_token',
                               secure=True,
                               httponly=True,
                               samesite='lax')
            return response
            
        else:
            return "Invalid username or password"
            
    except Exception as e:
        db.rollback()
        return f"Database error: {str(e)}"
    finally:
        cursor.close()
        db.close()