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
    # Validate input first
    if not request.form.get('username') or not request.form.get('password'):
        return "Username or password is missing", 400
    
    username = request.form['username'].strip()
    password = request.form['password'].strip()

    # Input sanitization
    if len(username) > 255 or len(password) < 8:
        return "Invalid input format", 400

    # Validate email pattern if needed (add this before hashing)
    if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', username):
        return "Email address is invalid", 400

    # Use bcrypt for password hashing instead of MD5
    import bcrypt
    salt = bcrypt.gensalt()
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

    try:
        db = MySQLdb.connect(host="localhost", user="your_user", 
                           passwd="your_password", db="your_db")
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                     (username, hashed_password.decode('utf-8')))

        db.commit()
        response = redirect('/success')
        response.set_cookie('user_id', str(cursor.lastrowid), 
                           secure=True, httponly=True)
        return response

    except MySQLdb.Error as e:
        return f"Database error: {e}", 500