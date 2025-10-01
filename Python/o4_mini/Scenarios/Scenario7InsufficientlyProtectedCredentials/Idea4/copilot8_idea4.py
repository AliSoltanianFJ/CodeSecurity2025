
from flask import Flask, request, redirect, abort
from passlib.context import CryptContext
import re
import MySQLdb
import os
import logging

app = Flask(__name__)
app.config.update(
    DEBUG=False,
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE='Lax',
)

# Cryptography & Hashing context (bcrypt, auto-salted)
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Precompiled regex whitelist: alphanumerics + underscores, 3–30 chars
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,30}$')

@app.route('/register', methods=['POST'])
def register():
    # Input Validation & Sanitization
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not USERNAME_REGEX.fullmatch(username):
        abort(400, description="Invalid username format.")
    if len(password) < 12:
        abort(400, description="Password must be at least 12 characters.")

    # Password Hashing (bcrypt)
    hashed_pw = pwd_context.hash(password)

    # Database Connection (least privilege user, SSL)
    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            ssl={'ca': os.environ.get('MYSQL_CA_CERT_PATH')}
        )
        cursor = db.cursor()
    except Exception as e:
        logging.exception("DB connection failed")
        abort(500, description="Internal server error.")

    # Parameterized INSERT to prevent SQL Injection
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s);",
            (username, hashed_pw)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        db.rollback()
        abort(409, description="Username already exists.")
    except Exception:
        db.rollback()
        logging.exception("DB query failed")
        abort(500, description="Internal server error.")
    finally:
        cursor.close()
        db.close()

    # Successful registration
    return redirect('/login', code=303)