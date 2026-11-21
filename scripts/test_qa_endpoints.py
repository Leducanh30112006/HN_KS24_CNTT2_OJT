import urllib.request
import urllib.error
import json

def test_cart_negative():
    req = urllib.request.Request(
        'http://172.18.0.5:8083/api/cart/items',
        data=json.dumps({'variantId': -1, 'quantity': 1}).encode('utf-8'),
        headers={'Content-Type': 'application/json', 'X-User-Id': '2', 'X-User-Roles': 'ROLE_BUYER'},
        method='POST'
    )
    try:
        resp = urllib.request.urlopen(req)
        print('Cart Negative (-1): UNEXPECTED SUCCESS', resp.status, resp.read().decode())
    except urllib.error.HTTPError as e:
        print('Cart Negative (-1): HTTP', e.code, e.read().decode())

def test_cart_nonexistent():
    req = urllib.request.Request(
        'http://172.18.0.5:8083/api/cart/items',
        data=json.dumps({'variantId': 999999999, 'quantity': 1}).encode('utf-8'),
        headers={'Content-Type': 'application/json', 'X-User-Id': '2', 'X-User-Roles': 'ROLE_BUYER'},
        method='POST'
    )
    try:
        resp = urllib.request.urlopen(req)
        print('Cart Nonexistent (999999999): UNEXPECTED SUCCESS', resp.status, resp.read().decode())
    except urllib.error.HTTPError as e:
        print('Cart Nonexistent (999999999): HTTP', e.code, e.read().decode())

def test_checkout_nonexistent():
    checkout_payload = {
        'items': [{'variantId': 999999999, 'quantity': 1}],
        'shippingAddress': '123 Nguyen Trai, Ha Noi',
        'recipientName': 'Nguyen Van A',
        'recipientPhone': '0912345678',
        'paymentMethod': 'COD'
    }
    req = urllib.request.Request(
        'http://172.18.0.5:8083/api/orders/checkout',
        data=json.dumps(checkout_payload).encode('utf-8'),
        headers={'Content-Type': 'application/json', 'X-User-Id': '2', 'X-User-Roles': 'ROLE_BUYER'},
        method='POST'
    )
    try:
        resp = urllib.request.urlopen(req)
        print('Checkout Nonexistent (999999999): UNEXPECTED SUCCESS', resp.status, resp.read().decode())
    except urllib.error.HTTPError as e:
        print('Checkout Nonexistent (999999999): HTTP', e.code, e.read().decode())

if __name__ == '__main__':
    print('--- Testing TC-CART-04 ---')
    test_cart_negative()
    test_cart_nonexistent()
    print('--- Testing TC-ORD-03 ---')
    test_checkout_nonexistent()
