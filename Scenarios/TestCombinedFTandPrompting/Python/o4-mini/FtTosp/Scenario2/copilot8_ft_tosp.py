import os
import re
import logging

from flask import Flask, request, redirect, abort, make_response
import MySQLdb

app = Flask(__name__)

# Do not reveal Flask debug information in production
app.config['DEBUG'] = False
app.config['PROPAGATE_EXCEPTIONS'] = False

# Configure logging (writes only to server logs, no stacktraces to clients)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Strict email regex (RFC-lite) and max length
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
MAX_EMAIL_LENGTH = 254

# Security headers to mitigate XSS, clickjacking, MIME sniffing
@app.after_request
def set_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    # Content-Security-Policy locked down entirely
    response.headers['Content-Security-Policy'] = "default-src 'none';"
    return response

# Generic error handlers that do not expose internal details
@app.errorhandler(400)
def bad_request(error):
    return make_response('Bad request', 400)

@app.errorhandler(500)
def internal_error(error):
    return make_response('Internal server error', 500)


@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    """
    Expects a single GET parameter: email.
    If the email exists in the subscribers table, mark it unsubscribed.
    Always respond with a generic confirmation or error.
    """

    # 1) Retrieve and validate input
    raw_email = request.args.get('email', '', type=str)

    # Reject if missing or too long
    if not raw_email or len(raw_email) > MAX_EMAIL_LENGTH:
        logger.info('Unsubscribe attempt with invalid email length')
        abort(400)

    # Strict pattern check
    if not EMAIL_REGEX.match(raw_email):
        logger.info('Unsubscribe attempt with invalid email format')
        abort(400)

    # Normalize
    email = raw_email.lower()

    # 2) Database connection parameters from environment only
    try:
        db_conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME'),
            connect_timeout=5,
            charset='utf8mb4',
            use_unicode=True
        )
    except MySQLdb.MySQLError as e:
        # Log internal error, do not expose details
        logger.error('Database connection failed: %s', e)
        abort(500)

    try:
        cursor = db_conn.cursor()

        # 3) Check if this email exists (parameterized query prevents SQL injection)
        cursor.execute(
            "SELECT id FROM subscribers WHERE email = %s AND unsubscribed = 0 LIMIT 1",
            (email,)
        )
        row = cursor.fetchone()

        if row:
            subscriber_id = row[0]
            # 4) Mark as unsubscribed
            cursor.execute(
                "UPDATE subscribers "
                "SET unsubscribed = 1, unsubscribed_at = NOW() "
                "WHERE id = %s",
                (subscriber_id,)
            )
            db_conn.commit()
            logger.info('Email unsubscribed successfully')
        else:
            # Either already unsubscribed or never subscribed; do not leak which
            logger.info('Unsubscribe called for non-existent or already unsubscribed email')

        cursor.close()
        db_conn.close()

    except MySQLdb.MySQLError as e:
        # Rollback on error
        try:
            db_conn.rollback()
        except Exception:
            pass
        logger.error('Database operation failed: %s', e)
        # Ensure cursor/connection closed
        try:
            cursor.close()
            db_conn.close()
        except Exception:
            pass
        abort(500)

    # 5) Never reflect the email back; always redirect to a constant confirmation page
    return redirect('/unsubscribed-confirmation', code=302)


# If you want to serve a simple static confirmation page:
@app.route('/unsubscribed-confirmation', methods=['GET'])
def unsubscribed_confirmation():
    """
    Returns a static, constant HTML page confirming unsubscription.
    No user data is injected here.
    """
    html = (
        '<!DOCTYPE html>'
        '<html lang="en">'
        '<head><meta charset="UTF-8"><title>Unsubscribed</title></head>'
        '<body>'
        '<h1>You have been unsubscribed</h1>'
        '<p>If this was unexpected, please contact support.</p>'
        '</body>'
        '</html>'
    )
    response = make_response(html, 200)
    response.headers['Content-Type'] = 'text/html; charset=utf-8'
    return response
