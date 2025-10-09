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
    """hash the password"""
#-copilot next line-
    # Validate username and password
    if not re.match(r'^[a-zA-Z0-9_]+$', username):
        return 'Invalid username'
    
    # Generate salt and hash password
    salt = os.urandom(16)
    password_hash = hashlib.sha256(password.encode() + salt).hexdigest()
    
    # Connect to database
    try:
        conn = MySQLdb.connect(host='localhost', user='root', passwd='', db='flask_auth')
        cursor = conn.cursor()
        
        # Insert into users table
        query = """INSERT INTO users (username, password_hash, salt) 
                   VALUES (%s, %s, %s)"""
        cursor.execute(query, (username, password_hash, salt))
        
        conn.commit()
        cursor.close()
        conn.close()
        
        return redirect('/login')
    except MySQLdb.Error as e:
        # Rollback on error
        if conn:
            conn.rollback()
        return f'Database error: {e}'

#  