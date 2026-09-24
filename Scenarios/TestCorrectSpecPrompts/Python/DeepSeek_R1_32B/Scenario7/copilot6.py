from flask import Flask, request, redirect
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash, check_password_hash
from flask.flash import Flash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    if not re.match(r'^\w+$', username):
        return 'Invalid username', 400
    if len(password) < 8:
        return 'Password too short', 400

    try:
        hashed_password = generate_password_hash(password)
        
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="user_db")
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            db.close()
            return 'Username already exists', 400
            
        # Insert new user
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", 
                     (username, hashed_password))
        db.commit()
        db.close()
        
        # Register successful, redirect to login page
        return redirect('/login')
        
    except MySQLdb.Error as e:
        # To prevent exposing sensitive information, we can log the error internally and return a generic message to the user.
        return f"Database error: {str(e)}"