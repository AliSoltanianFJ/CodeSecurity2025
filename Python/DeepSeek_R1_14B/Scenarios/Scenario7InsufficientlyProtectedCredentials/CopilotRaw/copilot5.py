from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Encode the password to bytes and hash it
    encoded_password = password.encode('utf-8')
    hashed_password = hashlib.sha256(encoded_password).hexdigest()
    
    # Check if username already exists
    db = MySQLdb.connect(host="localhost", user="root", passwd="", db="your_database_name", charset='utf8')
    cursor = db.cursor()
    
    # First check if username exists
    cursor.execute("SELECT user_id FROM users WHERE username = %s", (username,))
    result = cursor.fetchone()
    
    if result:
        return "Username already exists"
    
    try:
        # Insert new user into database
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", 
                     (username, hashed_password))
        db.commit()
        return redirect('/')
    except Exception as e:
        db.rollback()
        return "Registration failed: " + str(e)
    finally:
        db.close()