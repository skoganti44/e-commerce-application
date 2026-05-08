package main

func (a *App) RegisterRoutes() {
	r := a.Router

	// Auth & users
	r.POST("/register", a.Register)
	r.POST("/login", a.Login)
	r.GET("/users", a.FetchUsers)
	r.GET("/roles", a.FetchRoles)
	r.GET("/userRoles", a.FetchUserRoles)

	// Cart
	r.GET("/cart", a.FetchCart)
	r.POST("/cart/add", a.AddToCart)
	r.POST("/cart/item/update", a.UpdateCartItem)
	r.DELETE("/cart/item", a.RemoveCartItem)

	// Orders / payments
	r.GET("/orders", a.FetchOrders)
	r.GET("/payments", a.FetchPayments)

	// Products
	r.GET("/products", a.FetchProducts)
	r.POST("/product", a.SaveProduct)
	r.POST("/products", a.SaveProducts)

	// Shipping address
	r.POST("/shipping-address", a.SaveShippingAddress)
	r.GET("/shipping-address", a.GetLatestShippingAddress)

	// Checkout
	r.POST("/checkout", a.Checkout)
	r.POST("/counter/sale", a.RecordCounterSale)

	// Cleanup
	r.DELETE("/cleanup", a.Cleanup)

	// Sales
	r.GET("/sales/analytics", a.SalesAnalytics)

	// Kitchen
	r.GET("/kitchen/online-orders", a.FetchKitchenOnlineOrders)
	r.GET("/kitchen/instore-orders", a.FetchKitchenInStoreOrders)
	r.GET("/kitchen/daily-stock", a.FetchDailyStock)
	r.GET("/delivery/online-orders", a.FetchDeliveryOnlineOrders)
	r.GET("/bakery/instore-orders", a.FetchBakeryInStoreOrders)
	r.POST("/kitchen/daily-stock/:stockId/adjust", a.AdjustDailyStockPrepared)
	r.GET("/kitchen/supplies", a.FetchSupplies)
	r.GET("/kitchen/in-stock", a.FetchInStockSupplies)
	r.POST("/kitchen/supplies/:supplyId/request", a.RequestMoreSupply)
	r.POST("/kitchen/supplies", a.SaveSupply)
	r.POST("/kitchen/supplies/:supplyId/adjust", a.AdjustSupplyStock)
	r.POST("/kitchen/supplies/bulk-status", a.BulkUpdateSupplyStatuses)
	r.POST("/kitchen/supplies/seed", a.SeedSupplies)
	r.POST("/kitchen/order/:orderId/status", a.UpdateKitchenOrderStatus)

	// Tasks
	r.POST("/tasks", a.CreateTask)
	r.GET("/tasks", a.ListTasks)
	r.POST("/tasks/:taskId/status", a.UpdateTaskStatus)

	// Delivery trips
	r.POST("/delivery/trips", a.PickUpTrip)
	r.POST("/delivery/trips/:tripId/out", a.MarkOutForDelivery)
	r.POST("/delivery/trips/:tripId/deliver", a.MarkDelivered)
	r.POST("/delivery/trips/:tripId/fail", a.MarkTripFailed)
	r.GET("/delivery/trips", a.ListTrips)
	r.POST("/delivery/issues", a.LogIssue)
	r.GET("/delivery/issues", a.ListIssues)
	r.GET("/delivery/summary", a.ShiftSummary)

	// Management
	r.GET("/management/ops", a.ManagementOps)
	r.GET("/management/orders-audit", a.ManagementOrdersAudit)
	r.GET("/management/deliveries-audit", a.ManagementDeliveriesAudit)
	r.GET("/management/day-pnl", a.ManagementDayPnl)
	r.GET("/management/staff-performance", a.ManagementStaffPerformance)
	r.GET("/management/cash-reconciliation", a.CashReconciliation)
	r.GET("/management/supply-requests", a.FetchSupplyRequests)
	r.POST("/management/supplies/:supplyId/fulfill", a.FulfillSupply)

	// Generic team-supply request (any team)
	r.POST("/supplies/:supplyId/request", a.RequestSupplyByTeam)

	// Discount campaigns
	r.POST("/discount-campaigns", a.ProposeDiscountCampaign)
	r.GET("/discount-campaigns", a.ListDiscountCampaigns)
	r.POST("/discount-campaigns/:id/decision", a.DecideDiscountCampaign)

	// Order approvals
	r.POST("/orders/:orderId/flag-approval", a.FlagOrderForApproval)
	r.GET("/orders/pending-approval", a.ListOrdersPendingApproval)
	r.POST("/orders/:orderId/approval-decision", a.DecideOrderApproval)

	// Refund requests
	r.POST("/refund-requests", a.RaiseRefundRequest)
	r.GET("/refund-requests", a.ListRefundRequests)
	r.POST("/refund-requests/:id/decision", a.DecideRefundRequest)
}
