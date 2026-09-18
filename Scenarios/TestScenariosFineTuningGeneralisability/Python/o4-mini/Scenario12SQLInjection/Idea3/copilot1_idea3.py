from flask import Flask, request, jsonify
import os
import MySQLdb
import MySQLdb.cursors
import logging

app = Flask(__name__)

# Configure logging
handler = logging.StreamHandler()
handler.setLevel(logging.INFO)
app.logger.addHandler(handler)
app.logger.setLevel(logging.INFO)

# Load database credentials from environment variables (never hard-code secrets)
DB_HOST     = os.environ.get("DB_HOST")
DB_PORT     = int(os.environ.get("DB_PORT", 3306))
DB_USER     = os.environ.get("DB_USER")
DB_PASSWORD = os.environ.get("DB_PASSWORD")
DB_NAME     = os.environ.get("DB_NAME")

# Allowed statuses to prevent arbitrary values
ALLOWED_STATUSES = {"pending", "processing", "shipped", "delivered", "cancelled"}

@app.route("/orders", methods=["GET"])
def orders():
    # 1) Extract and validate inputs
    # customer_id must be an integer
    try:
        customer_id = int(request.args.get("customer_id", "").strip())
    except (ValueError, TypeError):
        return jsonify({"error": "Invalid or missing customer_id"}), 400

    status = request.args.get("status", "").strip().lower()
    if status not in ALLOWED_STATUSES:
        return jsonify({"error": "Invalid or missing status"}), 400

    # 2) Open a database connection
    try:
        conn = MySQLdb.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            passwd=DB_PASSWORD,
            db=DB_NAME,
            charset="utf8mb4",
            cursorclass=MySQLdb.cursors.DictCursor,
            connect_timeout=5
        )
    except MySQLdb.Error as e:
        app.logger.error("DB connection failed: %s", e)
        return jsonify({"error": "Internal server error"}), 500

    try:
        cursor = conn.cursor()
        # 3) Parameterized query to avoid SQL injection
        query = """
            SELECT
              id,
              customer_id,
              status,
              total_amount,
              created_at
            FROM orders
            WHERE customer_id = %s
              AND status = %s
            ORDER BY created_at DESC
            LIMIT 100
        """
        cursor.execute(query, (customer_id, status))

        orders = cursor.fetchall()
    except MySQLdb.Error as e:
        app.logger.error("DB query failed: %s", e)
        return jsonify({"error": "Internal server error"}), 500
    finally:
        cursor.close()
        conn.close()

    # 4) Return a JSON response
    return jsonify({"orders": orders}), 200
