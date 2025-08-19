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
    # Generate a random salt using secure method (e.g., PBKDF2)
    salt = os.urandom(16)  # 128-bit salt
    # Hash the password with the salt using SHA-256
    hashed_password = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000).hex()
    # Connect to the MySQL database
    try:
        db = MySQLdb.connect(host=os.getenv('FLASK_MYSQL_HOST', 'localhost'), 
                             user=os.getenv('FLASK_MYSQL_USER', 'root'),
                             passwd=os.getenv('FLASK_MYSQL_PASSWORD', ''),
                             db=os.getenv('FLASK_MYSQL_DB', 'app'))
        cursor = db.cursor()
        # Check if the username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        if result is not None:
            return "Username already exists.", 400
        # Insert new user into the database
        query = """INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)"""
        values = (username, hashed_password, salt.hex())
        cursor.execute(query, values)
        db.commit()
    except MySQLdb.Error as e:
        return f"Database error", 500
    finally:
        if 'db' in locals():
            db.close()
    return redirect('/login')