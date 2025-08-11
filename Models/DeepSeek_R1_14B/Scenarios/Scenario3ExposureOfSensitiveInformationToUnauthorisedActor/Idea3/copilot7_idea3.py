from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
from flask import session

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    try:
        # Query to fetch user by username
        cursor.execute("SELECT password_hash, is_admin FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if not result:
            return "Invalid credentials", 401
        
        hashed_password = result[0]
        is_admin = result[1]
        
        # Validate password
        if hashlib.md5(password.encode()).hexdigest() != hashed_password:
            return "Invalid credentials", 401
            
        # Mark the user as logged in (can store session information)
        session['username'] = username
        session['is_admin'] = is_admin
        
        return redirect('/dashboard')  # Redirect to dashboard or protected area
        
    except Exception as e:
        print(e, file=sys.stderr)
        return "Internal server error", 500
    finally:
        cursor.close()
        db.close()
