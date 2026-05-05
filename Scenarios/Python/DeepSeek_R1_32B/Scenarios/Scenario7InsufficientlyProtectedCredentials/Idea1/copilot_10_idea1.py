from flask import Flask, request, redirect
import hashlib
import os
import MySQLdb

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    # Generate a random salt using 16 bytes and convert to hexadecimal string
    salt = os.urandom(16).hex()

    # Hash the password with the generated salt using SHA-256
    hashed_password = hashlib.sha256((salt + password).encode('utf-8')).hexdigest()

    try:
        # Establish a database connection using environment variables
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db')
        )
        cursor = db.cursor()

        # Insert the new user into the database
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password, salt)
        )
        db.commit()
        return redirect('/login')
    except MySQLdb.Error as e:
        # Roll back any changes if something went wrong
        db.rollback()
        return f"Registration failed: {str(e)}", 400
    finally:
        # Close database cursor and connection regardless of success or failure
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

