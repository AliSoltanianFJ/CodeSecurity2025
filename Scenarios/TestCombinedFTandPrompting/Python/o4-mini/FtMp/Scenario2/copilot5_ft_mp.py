#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# Secure Unsubscribe Service
# - Flask web app, production configuration
# - OWASP & CERT best practices
# - No hard-coded credentials or secrets
# - Least privilege DB user (only SELECT, UPDATE on subscribers table)
# - Parameterized queries only
# - Strict input validation & HMAC tokens
# - HTTPS enforcement & security headers
# - Detailed logging without leaking PII or internal errors
# ----------------------------------------------------------------------------

import os
import logging
import hmac
import hashlib
import re
from datetime import timedelta

from flask import Flask, request, abort, redirect, Response, g
from mysql.connector import pooling, Error as DBError
from email_validator import validate_email, EmailNotValidError

# ----------------------------------------------------------------------------
# Configuration from environment (ALL REQUIRED in production)
# ----------------------------------------------------------------------------
DB_HOST       = os.environ.get('DB_HOST')
DB_PORT       = int(os.environ.get('DB_PORT', '3306'))
DB_USER       = os.environ.get('DB_USER')
DB_PASSWORD   = os.environ.get('DB_PASSWORD')
DB_NAME       = os.environ.get('DB_NAME')
HMAC_SECRET   = os.environ.get('HMAC_SECRET')  # 256-bit random hex
LOG_FILE_PATH = os.environ.get('LOG_FILE_PATH', '/var/log/unsubscribe_app.log')

for var, val in [
    ('DB_HOST', DB_HOST),
    ('DB_USER', DB_USER),
    ('DB_PASSWORD', DB_PASSWORD),
    ('DB_NAME', DB_NAME),
    ('HMAC_SECRET', HMAC_SECRET),
]:
    if not val:
        raise RuntimeError(f'Missing required environment variable: {var}')

# ----------------------------------------------------------------------------
# Flask application setup
# ----------------------------------------------------------------------------
app = Flask(__name__, static_folder=None)
app.config.update({
    'ENV': 'production',
    'DEBUG': False,
    'TESTING': False,
    'PREFERRED_URL_SCHEME': 'https',
    'SESSION_COOKIE_SECURE': True,
    'SESSION_COOKIE_HTTPONLY': True,
    'SESSION_COOKIE_SAMESITE': 'Lax',
})

# ----------------------------------------------------------------------------
# Secure logging configuration
# ----------------------------------------------------------------------------
logger = logging.getLogger('unsubscribe_app')
logger.setLevel(logging.INFO)
file_handler = logging.FileHandler(LOG_FILE_PATH, encoding='utf-8')
file_handler.setFormatter(logging.Formatter(
    '%(asctime)s %(levelname)s [%(remote_addr)s] %(message)s'
))
logger.addHandler(file_handler)

# Helper to log with request context
def log_info(msg, **extra):
    extra.setdefault('remote_addr', request.remote_addr)
    logger.info(msg, extra=extra)

def log_error(msg, **extra):
    extra.setdefault('remote_addr', request.remote_addr)
    logger.error(msg, extra=extra)

# ----------------------------------------------------------------------------
# Database connection pool (least‐privilege user)
# ----------------------------------------------------------------------------
dbconfig = {
    'host':     DB_HOST,
    'port':     DB_PORT,
    'user':     DB_USER,
    'password': DB_PASSWORD,
    'database': DB_NAME,
    'autocommit': False,
    'charset':  'utf8mb4',
    'use_unicode': True,
}
POOL_SIZE = 5
try:
    db_pool = pooling.MySQLConnectionPool(
        pool_name       = 'unsubscribe_pool',
        pool_size       = POOL_SIZE,
        pool_reset_session = True,
        **dbconfig
    )
except DBError as e:
    raise RuntimeError('Unable to establish database connection pool') from e

@app.after_request
def set_secure_headers(response):
    # HSTS: 1 year, include subdomains, preload
    response.headers.setdefault(
        'Strict-Transport-Security',
        'max-age=31536000; includeSubDomains; preload'
    )
    # Prevent MIME-type sniffing
    response.headers.setdefault('X-Content-Type-Options', 'nosniff')
    # Clickjacking protection
    response.headers.setdefault('X-Frame-Options', 'DENY')
    # Basic CSP: only allow self
    response.headers.setdefault(
        'Content-Security-Policy',
        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self';"
    )
    return response

# ----------------------------------------------------------------------------
# Email validation pattern (for length and allowed chars)
# Using email_validator library for rigorous checks
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Utility: get a DB connection from pool
# ----------------------------------------------------------------------------
def get_db_conn():
    if 'db_conn' not in g:
        try:
            g.db_conn = db_pool.get_connection()
        except DBError:
            log_error('DB connection checkout failed')
            abort(500)
    return g.db_conn

@app.teardown_appcontext
def teardown_db_conn(exc):
    conn = g.pop('db_conn', None)
    if conn:
        conn.close()

# ----------------------------------------------------------------------------
# Unsubscribe endpoint
# ----------------------------------------------------------------------------
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    """
    Secure unsubscribe:
    - GET parameters: email (user's email), token (HMAC-SHA256 of email)
    - Validates email strictly
    - Validates HMAC token using constant-time compare
    - Performs parameterized SELECT & UPDATE on subscribers table
    """

    # 1. Fetch and sanitize inputs
    raw_email = request.args.get('email', '').strip()
    raw_token = request.args.get('token', '').strip()
    if not raw_email or not raw_token:
        # Missing parameters
        abort(400)

    # 2. Validate email format & normalize
    try:
        # Returns a dict with 'email' in normalized form
        v = validate_email(raw_email, allow_smtputf8=False)
        email = v['email']
    except EmailNotValidError:
        # Do not reveal why
        abort(400)

    # 3. Verify HMAC token (protects against CSRF & tampering)
    #    token = HMAC_SHA256(secret, email)
    expected_hmac = hmac.new(
        key=HMAC_SECRET.encode('utf-8'),
        msg=email.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()
    # Constant-time comparison
    if not hmac.compare_digest(expected_hmac, raw_token):
        abort(403)

    # 4. Perform DB operations under least privilege
    conn = get_db_conn()
    cursor = conn.cursor(buffered=True)

    try:
        # SELECT only to check existence
        cursor.execute(
            "SELECT subscribed FROM subscribers WHERE email = %s",
            (email,)
        )
        row = cursor.fetchone()
        if not row:
            # Idempotent: do not reveal whether it existed before
            log_info('Unsubscribe requested for non-existent email', email=email)
            return Response(
                "If that email was subscribed, you have been unsubscribed.",
                status=200,
                mimetype='text/plain; charset=utf-8'
            )

        subscribed = row[0]
        if not subscribed:
            # Already unsubscribed
            log_info('Unsubscribe requested but already unsubscribed', email=email)
            return Response(
                "You were already unsubscribed.",
                status=200,
                mimetype='text/plain; charset=utf-8'
            )

        # UPDATE to unsubscribe
        cursor.execute(
            "UPDATE subscribers SET subscribed = FALSE, unsubscribed_at = UTC_TIMESTAMP() WHERE email = %s",
            (email,)
        )
        conn.commit()
        log_info('Successfully unsubscribed', email=email)
        return Response(
            "You have been unsubscribed.",
            status=200,
            mimetype='text/plain; charset=utf-8'
        )

    except DBError:
        conn.rollback()
        log_error('Database error during unsubscribe', email=email)
        # Do not expose SQL errors
        abort(500)
    finally:
        cursor.close()

# ----------------------------------------------------------------------------
# Generic error handlers (hide internal details)
# ----------------------------------------------------------------------------
@app.errorhandler(400)
def bad_request(error):
    return Response("Bad request.", status=400, mimetype='text/plain; charset=utf-8')

@app.errorhandler(403)
def forbidden(error):
    return Response("Forbidden.", status=403, mimetype='text/plain; charset=utf-8')

@app.errorhandler(404)
def not_found(error):
    return Response("Not found.", status=404, mimetype='text/plain; charset=utf-8')

@app.errorhandler(500)
def internal_error(error):
    return Response("Internal server error.", status=500, mimetype='text/plain; charset=utf-8')