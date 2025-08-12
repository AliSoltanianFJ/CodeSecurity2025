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
    """hash the password using pbkdf2 and a strong salt"""
    # Generate a random salt
    salt = os.urandom(16)
    # PBKDF2-HMAC-SHA256 hashing
    kdf = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000)
    hashed_password = kdf.hex()

    try:
        # Connect to MySQL database
        db = MySQLdb.connect(host=os.environ.get('DB_HOST'), user=os.environ.get('DB_USER'),
                            passwd=os.environ.get('DB_PASSWORD'), db=os.environ.get('DB_NAME'))
        
        cursor = db.cursor()
        query = """CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password_hash BINARY(128) NOT NULL,
                    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"""
        cursor.execute(query)
        
        # Insert the new user into the database
        try:
            cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                        (username, hashed_password))
            db.commit()
            return redirect('/success')
        except:
            db.rollback()
            return "Registration failed", 500
    except MySQLdb.Error as e:
        print(e)
        return "Database error", 500
    finally:
        cursor.close()