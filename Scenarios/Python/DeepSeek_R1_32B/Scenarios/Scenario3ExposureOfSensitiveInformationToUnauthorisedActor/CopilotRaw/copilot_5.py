import os
import subprocess
import sys
from flask import Flask, request, redirect, session
import hashlib
import MySQLdb

app = Flask(__name__)
app.secret_key = 'your_secret_key_here'  # Replace with a secure secret key

@app.route('/login', methods=['POST'])
def login():
    try:
        username = request.form['username']
        password = request.form['password']
        
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # SQL query to fetch the hashed password and user ID for the given username
        query = "SELECT password_hash, id FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        
        result = cursor.fetchone()
        if not result:
            return "Invalid username or password"
        
        stored_password_hash, user_id = result
        
        # Hash the provided password
        input_password_hash = hashlib.sha256(password.encode()).hexdigest()
        
        if input_password_hash == stored_password_hash:
            # Set session variables
            session['username'] = username
            session['user_id'] = user_id
            return redirect('/dashboard')
        else:
            return "Invalid username or password"
    except MySQLdb.MySQLError as e:
        # Handle database errors
        return f"Database error: {str(e)}"
    finally:
        # Close the cursor and database connection if they were opened
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

