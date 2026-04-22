import client from './client.js';

export const fetchUsers = () => client.get('/users').then((r) => r.data);

export const registerUser = (payload) =>
  client.post('/register', payload).then((r) => r.data);

export const loginUser = (payload) =>
  client.post('/login', payload).then((r) => r.data);

export const fetchRoles = (department) =>
  client
    .get('/roles', { params: department ? { department } : {} })
    .then((r) => r.data);

export const fetchCart = (userid) =>
  client.get('/cart', { params: { userid } }).then((r) => r.data);

export const addToCart = (payload) =>
  client.post('/cart/add', payload).then((r) => r.data);

export const updateCartItem = (userid, cartItemId, quantity) =>
  client
    .post('/cart/item/update', { userid, cartItemId, quantity })
    .then((r) => r.data);

export const removeCartItem = (userid, itemId) =>
  client
    .delete('/cart/item', { params: { userid, itemId } })
    .then((r) => r.data);

export const fetchPayments = (userid, includeAll = false) =>
  client
    .get('/payments', { params: { userid, includeAll } })
    .then((r) => r.data);

export const fetchProducts = () =>
  client.get('/products').then((r) => r.data);

export const saveProduct = (payload) =>
  client.post('/product', payload).then((r) => r.data);

export const saveProducts = (payload) =>
  client.post('/products', payload).then((r) => r.data);

export const cleanupForUser = (userid) =>
  client.delete('/cleanup', { params: { userid } }).then((r) => r.data);

export const saveShippingAddress = (userid, address) =>
  client
    .post('/shipping-address', { userid, address })
    .then((r) => r.data);

export const fetchShippingAddress = (userid) =>
  client
    .get('/shipping-address', { params: { userid } })
    .then((r) => r.data);

export const checkout = (payload) =>
  client.post('/checkout', payload).then((r) => r.data);
