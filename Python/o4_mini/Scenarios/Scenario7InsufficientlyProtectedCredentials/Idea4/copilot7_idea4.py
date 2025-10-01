
from flask import Flask, request, redirect
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash
app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    # validate inputs
    if not re.fullmatch(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password too short", 400
    # hash password
    hashed = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    # insert into DB using env creds and parameterized query
    conn = MySQLdb.connect(
        host=os.environ['DB_HOST'],
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        charset='utf8mb4'
    )
    try:
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO users (username, password) VALUES (%s, %s)",
                (username, hashed)
            )
            conn.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists", 409
    finally:
        conn.close()
    return redirect('/login'), 302