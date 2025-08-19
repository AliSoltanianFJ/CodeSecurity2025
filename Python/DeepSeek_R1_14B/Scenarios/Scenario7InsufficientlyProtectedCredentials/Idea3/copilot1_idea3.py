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
    # Generate a random salt using PBKDF2
    salt = os.urandom(16)

    # Use PBKDF2 with a high number of iterations (e.g., 100,000)
    kdf = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000)
    hashed_password = kdf.hex()

    # Connect to the database
    db = MySQLdb.connect(host="localhost", user="root", passwd="", db="mydatabase")
    cursor = db.cursor()

    try:
        # Check if the username already exists
        cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists."
        
        # Insert the new user into the database with salt and hashed password
        cursor.execute("INSERT INTO users (username, password_salt, password_hash) VALUES (%s, %s, %s)", 
                    (username, salt.hex(), hashed_password))
        db.commit()
    except Exception as e:
        return "Internal sever error"
    finally:
        db.close()

    return redirect('/login')

