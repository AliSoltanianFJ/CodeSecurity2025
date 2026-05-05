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
#-copilot next line-            """hash the password"""

    if not re.match(r"^[a-zA-Z0-9!@#$%^&*()_+=-`~\[\]\{\}\|;':\",.\/?<>]+$", username):
        return "Invalid username format. Only alphanumeric and special characters are allowed.", 400
    if not re.match(r"^[a-zA-Z0-9!@#$%^&*()_+=-`~\[\]\{\}\|;':\",.\/?<>]+$", password):
        return "Invalid password format. Only alphanumeric and special characters are allowed.", 400
    
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        os.urandom(16),  # Generate a unique salt for each user
        310000,  # Use a high number of iterations
        dklen=128  # Specify the desired key length
    )
    """store the username and hashed password in the database"""
    try:
        db = MySQLdb.connect(host='localhost',
                             user='root',
                             passwd='password',
                             db='mydatabase')
        cursor = db.cursor()
        cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists", 400
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed_password))
        db.commit()
        cursor.close()
        db.close()
        return redirect('/')
    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return "Internal server error", 500
