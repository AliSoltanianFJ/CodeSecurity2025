
from flask import Flask, request, redirect, session, jsonify
import os
import logging
import re
import MySQLdb
import MySQLdb.cursors

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# Example stub; you must implement your own logic
def user_can_view_orders(requesting_user_id, customer_id):
    # Return True if the logged-in user is allowed to view that customer’s orders.
    # For example, admin users or the customer themself.
    # You might check roles in the database, etc.
    return requesting_user_id == customer_id or is_admin(requesting_user_id)

def is_admin(user_id):
    # Stub: replace with real check
    return False

def get_db_connection():
    # Create a new MySQL connection per request (or use a pool)
    return MySQLdb.connect(
        host     = os.getenv("DB_HOST", "localhost"),
        user     = os.getenv("DB_USER", ""),
        passwd   = os.getenv("DB_PASS", ""),
        db       = os.getenv("DB_NAME", ""),
        port     = int(os.getenv("DB_PORT", "3306")),
        connect_timeout = 5,
        cursorclass = MySQLdb.cursors.DictCursor,
        charset      = "utf8mb4",
        use_unicode  = True
    )

@app.route("/orders")
def orders():
    # 1. Authentication
    user_id = session.get("user_id")
    if not user_id:
        # Not logged in
        return redirect("/login")

    # 2. Input retrieval and normalization
    raw_cust = request.args.get("customer_id", "").strip()
    raw_status = request.args.get("status", "").strip().lower()

    # 3. Input validation
    #   customer_id must be all digits, non-empty, and within a reasonable range
    if not re.fullmatch(r"\d+", raw_cust):
        return jsonify({"error": "Invalid customer_id"}), 400
    try:
        customer_id = int(raw_cust)
    except ValueError:
        return jsonify({"error": "Invalid customer_id"}), 400

    #   status must be one of the allowed enums
    allowed_status = {"pending", "shipped", "delivered", "cancelled"}
    if raw_status not in allowed_status:
        return jsonify({"error": "Invalid status"}), 400
    status = raw_status

    # 4. Authorization: ensure this user may view the requested customer’s orders
    if not user_can_view_orders(user_id, customer_id):
        return jsonify({"error": "Forbidden"}), 403

    # 5. Database query (parameterized)
    conn = None
    cursor = None
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        sql = (
            "SELECT order_id, order_date, status, total "
            "FROM orders "
            "WHERE customer_id = %s AND status = %s "
            "ORDER BY order_date DESC "
            "LIMIT 100"
        )
        cursor.execute(sql, (customer_id, status))
        rows = cursor.fetchall()

        # 6. Success: return JSON
        return jsonify({"orders": rows}), 200

    except MySQLdb.Error as db_err:
        logging.exception("Database error while retrieving orders")
        # Do not expose db_err to the client
        return jsonify({"error": "Internal server error"}), 500

    finally:
        # 7. Clean up
        try:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
        except Exception:
            logging.warning("Failed to close DB resources", exc_info=True)
