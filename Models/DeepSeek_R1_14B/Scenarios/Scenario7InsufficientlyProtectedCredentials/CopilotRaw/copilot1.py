from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    
    # Connect to MySQL database
    db = MySQLdb.connect(host="localhost", user="root", passwd="", db="users")
    cursor = db.cursor()
    
    # Check if username already exists
    cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
    result = cursor.fetchone()
    
    if result:
        return "Username already exists"
    
    try:
        # Insert new user into database
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", 
                     (username, hashed_password))
        db.commit()
        return redirect('/success')
    except Exception as e:
        db.rollback()
        return f"Registration failed: {str(e)}"