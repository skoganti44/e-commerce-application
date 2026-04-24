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

export const fetchKitchenOnlineOrders = () =>
  client.get('/kitchen/online-orders').then((r) => r.data);

export const fetchKitchenInStoreOrders = () =>
  client.get('/kitchen/instore-orders').then((r) => r.data);

export const fetchDailyStock = () =>
  client.get('/kitchen/daily-stock').then((r) => r.data);

export const updateKitchenOrderStatus = (orderId, kitchenStatus, reason) =>
  client
    .post(`/kitchen/order/${orderId}/status`, { kitchenStatus, reason })
    .then((r) => r.data);

export const fetchDeliveryOnlineOrders = () =>
  client.get('/delivery/online-orders').then((r) => r.data);

export const fetchBakeryInStoreOrders = () =>
  client.get('/bakery/instore-orders').then((r) => r.data);

export const adjustDailyStockPrepared = (stockId, delta) =>
  client
    .post(`/kitchen/daily-stock/${stockId}/adjust`, { delta })
    .then((r) => r.data);

export const fetchSupplies = () =>
  client.get('/kitchen/supplies').then((r) => r.data);

export const saveSupply = (payload) =>
  client.post('/kitchen/supplies', payload).then((r) => r.data);

export const adjustSupplyStock = (supplyId, delta, note) =>
  client
    .post(`/kitchen/supplies/${supplyId}/adjust`, { delta, note })
    .then((r) => r.data);

export const seedSupplies = () =>
  client.post('/kitchen/supplies/seed').then((r) => r.data);

export const bulkUpdateSupplyStatuses = (updates) =>
  client
    .post('/kitchen/supplies/bulk-status', { updates })
    .then((r) => r.data);

export const fetchSupplyRequests = () =>
  client.get('/management/supply-requests').then((r) => r.data);

export const fulfillSupply = (supplyId, receivedQty, note) =>
  client
    .post(`/management/supplies/${supplyId}/fulfill`, { receivedQty, note })
    .then((r) => r.data);

export const fetchInStockSupplies = () =>
  client.get('/kitchen/in-stock').then((r) => r.data);

export const requestMoreSupply = (supplyId, requestedQty, urgency) =>
  client
    .post(`/kitchen/supplies/${supplyId}/request`, { requestedQty, urgency })
    .then((r) => r.data);
