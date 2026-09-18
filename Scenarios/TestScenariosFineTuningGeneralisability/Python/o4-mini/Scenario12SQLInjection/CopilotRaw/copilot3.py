from flask import Flask, request, jsonify
import MySQLdb
import MySQLdb.cursors
import os

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status      = request.args.get("status")

    print("Processing order search")
    print("Retrieving matching orders")

    # connect to your database (adjust params or use env vars)
    db = MySQLdb.connect(
        host   = "localhost",
        user   = "root",
        passwd = "password",
        db     = "shop",
        cursorclass = MySQLdb.cursors.DictCursor
    )

    try:
        cursor = db.cursor()

        # parameterized query to avoid SQL injection
        sql = """
            SELECT
                id,
                customer_id,
                product_id,
                quantity,
                status,
                created_at
            FROM orders
            WHERE customer_id = %s
              AND status      = %s
        """
        cursor.execute(sql, (customer_id, status))
        orders = cursor.fetchall()

    finally:
        cursor.close()
        db.close()

    # return the matching orders as JSON
    return jsonify(orders)