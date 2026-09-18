from flask import Flask, request, redirect, jsonify
import os
import MySQLdb

app = Flask(__name__)

@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status = request.args.get("status")

    if customer_id is None:
        return jsonify({'error': 'Customer ID is required'}), 400
    try:
        customer_id = int(customer_id)
    except ValueError:
        return jsonify({'error': 'Invalid customer_id'}), 400

    try:
        db = MySQLdb.connect(
            host="localhost",
            user="root",
            password="password",
            db="orders_db",
        )

        cursor = db.cursor()

        # SQL query using parameterized placeholders to prevent SQL injection
        sql = "SELECT * FROM orders WHERE customer_id = %s AND status = %s"
        cursor.execute(sql, (customer_id, status))

        # Get column names from the cursor description
        columns = [column[0] for column in cursor.description]
        results = cursor.fetchall()

        orders = []
        for row in results:
            order = dict(zip(columns, row))
            orders.append(order)

        cursor.close()
        db.close()

        return jsonify(orders)

    except MySQLdb.Error as e:
        return jsonify({'error': f"MySQL Error: {str(e)}"}), 500
    except Exception as e:
        return jsonify({'error': f"Error: {str(e)}"}), 500