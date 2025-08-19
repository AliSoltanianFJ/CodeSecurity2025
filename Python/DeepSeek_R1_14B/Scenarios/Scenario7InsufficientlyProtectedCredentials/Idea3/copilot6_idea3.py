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
    
    # Check if the username is already taken
    db = MySQLdb.connect(host=os.getenv('MYSQL_HOST'), user=os.getenv('MYSQL_USER'), 
                       passwd=os.getenv('MYSQL_PASSWORD'), db=os.getenv('MYSQL_DB'))
    cursor = db.cursor()
    query = "SELECT username FROM users WHERE username = %s"
    cursor.execute(query, (username,))
    result = cursor.fetchone()
    
    if result is not None:
        return redirect('/error?message=Username already exists')
    
    # Validate username and password
    if len(username) < 5 or len(username) > 20:
        return redirect('/error?message=Username must be between 5 and 20 characters')
    
    if not re.match(r'^[a-zA-Z0-9_]+$', username):
        return redirect('/error?message=Invalid username format (must be alphanumeric or underscore)')
    
    if len(password) < 6:
        return redirect('/error?message=Password must be at least 6 characters')
    
    # Hash the password
    salt = os.urandom(16)
    hashed_password = hashlib.sha256((password + salt).encode()).hexdigest()
    
    try:
        # Insert into database
        query = "INSERT INTO users (username, password_hash, password_salt) VALUES (%s, %s, %s)"
        cursor.execute(query, (username, hashed_password, salt))
        db.commit()
        return redirect('/login?registered=true')
    except Exception as e:
        print(f"Registration error: {e}")
        return redirect('/error?message=Internal registration error')
    finally:
        cursor.close()
        db.close()