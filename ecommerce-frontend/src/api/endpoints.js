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

export const recordCounterSale = (payload) =>
  client.post('/counter/sale', payload).then((r) => r.data);

export const fetchSalesAnalytics = (from, to) =>
  client
    .get('/sales/analytics', { params: { from, to } })
    .then((r) => r.data);

export const createTask = (payload) =>
  client.post('/tasks', payload).then((r) => r.data);

export const fetchTasks = (params = {}) =>
  client.get('/tasks', { params }).then((r) => r.data);

export const updateTaskStatus = (taskId, payload) =>
  client.post(`/tasks/${taskId}/status`, payload).then((r) => r.data);

export const pickUpDeliveryTrip = (orderId, driverId) =>
  client
    .post('/delivery/trips', { orderId, driverId })
    .then((r) => r.data);

export const markTripOutForDelivery = (tripId, driverId) =>
  client
    .post(`/delivery/trips/${tripId}/out`, { driverId })
    .then((r) => r.data);

export const markTripDelivered = (tripId, payload) =>
  client
    .post(`/delivery/trips/${tripId}/deliver`, payload)
    .then((r) => r.data);

export const markTripFailed = (tripId, payload) =>
  client
    .post(`/delivery/trips/${tripId}/fail`, payload)
    .then((r) => r.data);

export const fetchDeliveryTrips = (driverId, status) =>
  client
    .get('/delivery/trips', { params: status ? { driverId, status } : { driverId } })
    .then((r) => r.data);

export const logDeliveryIssue = (payload) =>
  client.post('/delivery/issues', payload).then((r) => r.data);

export const fetchDeliveryIssues = (driverId) =>
  client
    .get('/delivery/issues', { params: { driverId } })
    .then((r) => r.data);

export const fetchDeliveryShiftSummary = (driverId, from, to) =>
  client
    .get('/delivery/summary', {
      params: from && to ? { driverId, from, to } : { driverId },
    })
    .then((r) => r.data);

export const fetchManagementOps = () =>
  client.get('/management/ops').then((r) => r.data);

export const fetchManagementOrdersAudit = (params = {}) =>
  client.get('/management/orders-audit', { params }).then((r) => r.data);

export const fetchManagementDeliveriesAudit = (params = {}) =>
  client.get('/management/deliveries-audit', { params }).then((r) => r.data);

export const fetchManagementDayPnl = (date) =>
  client
    .get('/management/day-pnl', { params: date ? { date } : {} })
    .then((r) => r.data);

export const fetchManagementStaffPerformance = (from, to) =>
  client
    .get('/management/staff-performance', {
      params: from && to ? { from, to } : {},
    })
    .then((r) => r.data);

export const raiseRefundRequest = (payload) =>
  client.post('/refund-requests', payload).then((r) => r.data);

export const fetchRefundRequests = (status) =>
  client
    .get('/refund-requests', { params: status ? { status } : {} })
    .then((r) => r.data);

export const decideRefundRequest = (id, payload) =>
  client.post(`/refund-requests/${id}/decision`, payload).then((r) => r.data);

export const flagOrderForApproval = (orderId, notes) =>
  client
    .post(`/orders/${orderId}/flag-approval`, { notes: notes || null })
    .then((r) => r.data);

export const fetchOrdersPendingApproval = () =>
  client.get('/orders/pending-approval').then((r) => r.data);

export const decideOrderApproval = (orderId, payload) =>
  client.post(`/orders/${orderId}/approval-decision`, payload).then((r) => r.data);

export const proposeDiscountCampaign = (payload) =>
  client.post('/discount-campaigns', payload).then((r) => r.data);

export const fetchDiscountCampaigns = (status) =>
  client
    .get('/discount-campaigns', { params: status ? { status } : {} })
    .then((r) => r.data);

export const decideDiscountCampaign = (id, payload) =>
  client.post(`/discount-campaigns/${id}/decision`, payload).then((r) => r.data);

export const requestSupplyByTeam = (supplyId, requestedQty, urgency, team) =>
  client
    .post(`/supplies/${supplyId}/request`, { requestedQty, urgency, team })
    .then((r) => r.data);

export const fetchCashReconciliation = (date, openingFloat, countedCash) => {
  const params = {};
  if (date) params.date = date;
  if (openingFloat !== undefined && openingFloat !== null && openingFloat !== '')
    params.openingFloat = openingFloat;
  if (countedCash !== undefined && countedCash !== null && countedCash !== '')
    params.countedCash = countedCash;
  return client.get('/management/cash-reconciliation', { params }).then((r) => r.data);
};
