from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)
app.secret_key = os.urandom(24)  # Required for sessions

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    try:
        # Sanitize input and prepare the SQL query
        cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
        
        if cursor.rowcount > 0:
            # Fetch the stored password hash
            user_data = cursor.fetchone()
            stored_hash = user_data[2]  # Assuming password hash is in third column
            
            # Hash the input password
            salt = user_data[3]  # Assuming salt is in fourth column
            hashed_password = hashlib.sha256((password + salt).encode()).hexdigest()
            
            if hashed_password == stored_hash:
                session['username'] = username  # Start a session
                return redirect('/dashboard')   # Redirect to dashboard on success
            else:
                return redirect('/login?error=invalid_credentials')
        else:
            return redirect('/login?error=username_not_found')
    except MySQLdb.Error as e:
        print(f"An error occurred: {e}")
        return redirect('/login?error=db_error')
    finally:
        db.close()  # Ensure database connection is closed

