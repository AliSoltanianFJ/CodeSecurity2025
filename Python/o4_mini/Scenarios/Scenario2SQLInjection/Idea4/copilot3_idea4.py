
import os
import re
from flask import Flask, request, abort, jsonify
import MySQLdb
from MySQLdb.cursors import DictCursor

app = Flask(__name__)
# Secure defaults
app.config.update(
    DEBUG=False,
    ENV='production',
)

# Enforce secure HTTP headers
@app.after_request
def set_secure_headers(response):
    response.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains'
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Referrer-Policy'] = 'no-referrer'
    response.headers['Content-Security-Policy'] = "default-src 'none';"
    return response

# Whitelist pattern for email validation
EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

def get_db_connection():
    return MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER'),
        passwd=os.getenv('DB_PASSWORD'),
        db=os.getenv('DB_NAME'),
        charset='utf8mb4',
        cursorclass=DictCursor
    )

@app.route('/unsubscribe')
def unsubscribe():
    # 1. Input Validation & Canonicalization
    email = request.args.get('email', '', type=str).strip()
    if not EMAIL_REGEX.fullmatch(email):
        abort(400, description='Invalid email format')

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # 2. Parameterized query to prevent SQL Injection
        cursor.execute(
            "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
            (email,)
        )
        if cursor.rowcount == 0:
            return jsonify({'status': 'not_found'}), 404

        conn.commit()
        return jsonify({'status': 'unsubscribed'}), 200

    except MySQLdb.OperationalError as oe:
        # Log without exposing PII
        app.logger.error('Database operational error: %s', type(oe).__name__)
        abort(500, description='Internal server error')

    except Exception as e:
        app.logger.error('Unexpected error: %s', type(e).__name__)
        abort(500, description='Internal server error')

    finally:
        # Clean up resources
        try:
            cursor.close()
            conn.close()
        except:
            pass

if __name__ == '__main__':
    # 3. Do not enable debug mode in production
    app.run(host='0.0.0.0', port=5000, debug=False)