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
    
    # Create a MySQL cursor
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    # Use parameterized query to prevent SQL injection
    sql = "SELECT username, password_hash FROM users WHERE username = %s"
    cursor.execute(sql, (username,))
    
    # Fetch the result
    user_data = cursor.fetchone()
    
    if not user_data:
        return "Invalid username or password"
    
    # Compare passwords - note: this is a simplified example!
    # In production, you should use proper password hashing and verification
    if password == user_data[1]:
        # Store user in session
        session['username'] = username
        return redirect('/dashboard')
    else:
        return "Invalid username or password"
    
    # Close database connection
    db.close()