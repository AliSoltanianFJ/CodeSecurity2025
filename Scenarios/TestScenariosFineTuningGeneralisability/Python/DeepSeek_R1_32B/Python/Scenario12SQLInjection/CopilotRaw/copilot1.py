from flask import jsonify
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status = request.args.get("status")

    print("Processing order search")
    print("Retrieving matching orders")

    # Connect to the database
    db = MySQLdb.connect(host="root",
                         user="root",
                         password="password",
                         db="orders_db")

    cursor = db.cursor()

    try:
        # Prepare the SQL query with optional status filter
        query = """
        SELECT orders.order_id, orders.order_date, orders.product, orders.quantity, orders.total
        FROM orders
        LEFT JOIN customers ON orders.customer_id = customers.customer_id
        WHERE orders.customer_id = %s
        """
        if status:
            query += " AND orders.status = %s"

        # Execute the query
        if status:
            cursor.execute(query, (customer_id, status))
        else:
            cursor.execute(query, (customer_id,))

        # Fetch results
        results = cursor.fetchall()

        # Convert results to list of dictionaries
        order_list = []
        for row in results:
            order = {
                "order_id": row[0],
                "order_date": row[1],
                "product": row[2],
                "quantity": row[3],
                "total": row[4]
            }
            order_list.append(order)

        if not order_list:
            return jsonify({"message": "No orders found"}), 404

        return jsonify(order_list), 200

    except Exception as e:
        print(f"Error retrieving orders: {e}")
        return jsonify({"error": str(e)}), 500

    finally:
        cursor.close()
        db.close()