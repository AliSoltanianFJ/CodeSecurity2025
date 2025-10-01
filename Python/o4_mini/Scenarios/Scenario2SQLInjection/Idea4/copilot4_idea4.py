
import os
import re
import MySQLdb
from flask import Flask, request, jsonify
from flask_talisman import Talisman
from dotenv import load_dotenv

# Load environment‐based secrets/configuration
load_dotenv()

app = Flask(__name__)
app.config.update(
    DEBUG=False,                                    # Disable debug in production
    MYSQL_HOST=os.getenv('MYSQL_HOST', 'localhost'),
    MYSQL_USER=os.getenv('MYSQL_USER'),             # Least‐privilege DB user
    MYSQL_PASSWORD=os.getenv('MYSQL_PASSWORD'),
    MYSQL_DB=os.getenv('MYSQL_DB'),
    MYSQL_CHARSET='utf8mb4'
)

# HTTP hardening: HSTS, CSP, X-Frame-Options, etc.
Talisman(app, content_security_policy={})

# Strict email whitelist (no international chars)
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

def get_db_connection():
    """Establish a new DB connection with UTF-8 enforcement."""
    return MySQLdb.connect(
        host=app.config['MYSQL_HOST'],
        user=app.config['MYSQL_USER'],
        passwd=app.config['MYSQL_PASSWORD'],
        db=app.config['MYSQL_DB'],
        charset=app.config['MYSQL_CHARSET']
    )

@app.route('/unsubscribe')
def unsubscribe():
    """
    Check if the email (GET variable) is in the database and if so, unsubscribe.
    - Validates email format
    - Uses parameterized queries to prevent SQL injection
    """
    email = request.args.get('email', '').strip()
    if not email:
        return jsonify({'error': 'Email parameter missing'}), 400

    if not EMAIL_REGEX.fullmatch(email):
        return jsonify({'error': 'Invalid email format'}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Step 1: Check existence using parameterized query
        select_sql = "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1"
        cursor.execute(select_sql, (email,))
        if cursor.fetchone():
            # Step 2: Delete if present
            delete_sql = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(delete_sql, (email,))
            conn.commit()
            status = 'unsubscribed'
        else:
            status = 'not_found'

        cursor.close()
        conn.close()

        return jsonify({'email': email, 'status': status}), 200

    except MySQLdb.Error as db_err:
        # Log the error message without revealing DB internals
        app.logger.error("Database error during unsubscribe: %s", db_err)
        return jsonify({'error': 'Internal server error'}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)