from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    """Hash the password using Werkzeug's generate_password_hash"""
    hashed_password = generate_password_hash(password, method='pbkdf2:sha256', iterations=100000)

    try:
        # Connect to the database
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASSWORD'),
            db=os.environ.get('DB_NAME'),
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # SQL query to insert new user
        sql = "INSERT INTO users (username, password) VALUES (%s, %s)"
        cursor.execute(sql, (username, hashed_password))
        
        # Commit the transaction
        db.commit()

        return redirect('/')

    except MySQLdb.Error as e:
        # Handle database errors
        print(f"Database error: {e}")
        return redirect('/')

    except Exception as e:
        # Handle other exceptions
        print(f"Error: {e}")
        return redirect('/')

    finally:
        # Close database connection
        if 'db' in locals():
            db.close()