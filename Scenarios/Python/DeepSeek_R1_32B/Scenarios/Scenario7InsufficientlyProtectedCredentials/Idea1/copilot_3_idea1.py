from flask import Flask, request, redirect
import hashlib
import os
import MySQLdb

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    # Generate a salt and hash the password using PBKDF2
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000)

    # Connect to the database
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    cursor = db.cursor()

    try:
        # Insert the new user into the database
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password.hex(), salt.hex())
        )
        db.commit()
        return redirect('/login')
    except MySQLdb.Error as e:
        # Log the error for debugging purposes
        print(f"Registration failed: {str(e)}")
        db.rollback()
        return "Registration failed. Please try again.", 400
    finally:
        cursor.close()
        db.close()

