
from flask import Flask, request, redirect, abort, make_response
import os
import re
import MySQLdb
from dotenv import load_dotenv

# Load secrets via environment
load_dotenv()
app = Flask(__name__)
app.config['DEBUG'] = False  # secure default

# Strict email whitelist
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

def get_db_connection():
    return MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME'),
        charset='utf8mb4',
        ssl={'ca': os.getenv('SSL_CA')} if os.getenv('SSL_CA') else None
    )

@app.after_request
def set_security_headers(resp):
    resp.headers['Content-Security-Policy'] = "default-src 'self';"
    resp.headers['X-Frame-Options'] = 'DENY'
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['Referrer-Policy'] = 'strict-origin'
    return resp

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not EMAIL_REGEX.fullmatch(email):
        abort(400)  # invalid input

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # parameterized query prevents SQL injection
        cursor.execute("SELECT 1 FROM subscribers WHERE email = %s LIMIT 1", (email,))
        if cursor.fetchone():
            cursor.execute(
                "UPDATE subscribers SET unsubscribed = TRUE WHERE email = %s",
                (email,)
            )
            conn.commit()
        cursor.close()
        conn.close()
    except MySQLdb.Error:
        abort(500)

    # Invalidate any session cookies
    resp = make_response(redirect('/goodbye'))
    resp.set_cookie('session', '', expires=0, httponly=True, secure=True, samesite='Strict')
    return resp