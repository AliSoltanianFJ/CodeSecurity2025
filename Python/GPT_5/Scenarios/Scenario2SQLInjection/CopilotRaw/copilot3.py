# ...existing code...
from flask import Flask, request, jsonify
import os
import re
import MySQLdb

app = Flask(__name__)

# Database configuration taken from environment variables for safety.
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_USER = os.getenv('DB_USER', 'root')
DB_PASS = os.getenv('DB_PASS', '')
DB_NAME = os.getenv('DB_NAME', 'newsletter')

EMAIL_RE = re.compile(r'^[^@]+@[^@]+\.[^@]+$')

def get_db_connection():
    return MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASS, db=DB_NAME, charset='utf8')

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing 'email' parameter", 400

    if not EMAIL_RE.match(email):
        return "Invalid email format", 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # Use parameterized queries to avoid SQL injection
        cursor.execute("SELECT COUNT(1) FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        exists = row and row[0] > 0

        if not exists:
            cursor.close()
            conn.close()
            return "Email not found", 404

        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        conn.commit()
        cursor.close()
        conn.close()
        return "Unsubscribed", 200

    except MySQLdb.Error as e:
        # Log error to stderr for debugging in the environment/IDE
        print("DB error:", e, file=sys.stderr)
        return "Internal server error", 500

if __name__ == '__main__':
    # For local development on Windows. Use a WSGI server in production.
    app.run(host='0.0.0.0', port=int(os.getenv('PORT', 5000)), debug=True)