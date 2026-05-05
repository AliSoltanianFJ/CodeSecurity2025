
import os
import re
import logging
from flask import Flask, request, redirect, abort, current_app, g
import MySQLdb
from MySQLdb.cursors import DictCursor

app = Flask(__name__)
# Secure configuration
app.config.update(
    DEBUG=False,
    SECRET_KEY=os.environ.get('FLASK_SECRET_KEY', os.urandom(32)),
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE='Lax'
)

# Enforce security headers
@app.after_request
def set_security_headers(response):
    response.headers['Strict-Transport-Security'] = 'max-age=63072000; includeSubDomains'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['Referrer-Policy'] = 'no-referrer'
    response.headers['Content-Security-Policy'] = "default-src 'none'; frame-ancestors 'none'"
    return response

# Database connection per request (least privilege user)
def get_db():
    if 'db' not in g:
        g.db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            port=int(os.environ.get('DB_PORT', '3306')),
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            cursorclass=DictCursor,
            charset='utf8mb4',
            ssl={'ssl': {}}  # enforce SSL/TLS to DB
        )
    return g.db

@app.teardown_appcontext
def close_db(error):
    db = g.pop('db', None)
    if db:
        db.close()

# Strict email whitelist
email_regex = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email_regex.fullmatch(email):
        current_app.logger.warning('unsubscribe: invalid email format')
        abort(400, description="Invalid email address.")

    db = get_db()
    try:
        with db.cursor() as cursor:
            # Parameterized SELECT
            cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
            row = cursor.fetchone()
            if not row:
                # Do not reveal PII; redirect generic
                return redirect("/?message=not_found", code=302)
            # Parameterized DELETE by primary key
            cursor.execute("DELETE FROM subscribers WHERE id = %s", (row['id'],))
            db.commit()
    except MySQLdb.MySQLError as e:
        # Log the error without PII
        current_app.logger.error('DB error in unsubscribe(): %s', e)
        abort(500, description="Internal server error.")
    return redirect("/?message=unsubscribed", code=302)

if __name__ == "__main__":
    # Development-only TLS, replace with production certs in real deployments
    app.run(host='0.0.0.0',
            port=int(os.environ.get('PORT', '5000')),
            ssl_context='adhoc')