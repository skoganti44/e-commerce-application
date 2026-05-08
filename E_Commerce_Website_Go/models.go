package main

import (
	"database/sql"
	"time"

	"github.com/shopspring/decimal"
)

// All models mirror the Postgres schema in sql/01_schema.sql.

type User struct {
	UserID    int          `json:"userid"`
	Name      *string      `json:"name"`
	Email     *string      `json:"email"`
	Password  *string      `json:"password,omitempty"`
	CreatedAt sql.NullTime `json:"-"`
}

type Role struct {
	ID         int     `json:"id"`
	FullName   string  `json:"fullName"`
	Role       string  `json:"role"`
	Department *string `json:"department"`
}

type UserRole struct {
	UserRoleID int `json:"userroleid"`
	UserID     int `json:"userid"`
	RoleID     int `json:"roleid"`
}

type Product struct {
	ID                  int64
	Name                *string
	Description         *string
	Price               *decimal.Decimal
	Stock               *int
	CategoryID          *int64
	CreatedByUserID     *int
	SupportedFlours     *string
	SupportedSweeteners *string
}

type Category struct {
	ID          int64
	Name        *string
	Description *string
}

type Cart struct {
	ID        int64
	UserID    *int
	CreatedAt sql.NullTime
}

type CartItem struct {
	ID               int64
	CartID           *int64
	ProductID        *int64
	Quantity         *int
	Customization    *string
	FlourType        *string
	SweetenerPercent *int
	SweetenerType    *string
}

type Order struct {
	ID                int64
	UserID            *int
	TotalAmount       *decimal.Decimal
	Status            *string
	CreatedAt         sql.NullTime
	Channel           *string
	KitchenStatus     *string
	CustomerNotes     *string
	KitchenNotes      *string
	ApprovalNotes     *string
	ApprovalStatus    *string
	ApprovedAt        sql.NullTime
	RequiresApproval  *bool
	ApprovedByUserID  *int
}

type OrderItem struct {
	ID               int64
	OrderID          *int64
	ProductID        *int64
	Quantity         *int
	Price            *decimal.Decimal
	Customization    *string
	FlourType        *string
	SweetenerPercent *int
	SweetenerType    *string
}

type Payment struct {
	ID            int64
	OrderID       *int64
	PaymentMethod *string
	Status        *string
	Amount        *decimal.Decimal
	CreatedAt     sql.NullTime
}

type ShippingAddress struct {
	ID           int64
	AddressType  *string
	City         *string
	Country      *string
	CreatedAt    sql.NullTime
	FullName     *string
	Instructions *string
	Landmark     *string
	Line1        *string
	Line2        *string
	Phone        *string
	Pincode      *string
	State        *string
	OrderID      *int64
	UserID       *int
}

type Supply struct {
	ID               int64
	Category         *string
	CurrentStock     *decimal.Decimal
	Name             string
	Notes            *string
	Threshold        *decimal.Decimal
	Unit             string
	UpdatedAt        sql.NullTime
	OrderStatus      *string
	RequestedAt      sql.NullTime
	InStock          *decimal.Decimal
	RequestedQty     *decimal.Decimal
	RequestedByTeam  *string
}

type DailyStock struct {
	ID            int64
	PreparedCount *int
	StockDate     *time.Time
	TargetCount   *int
	ProductID     *int64
}

type DeliveryTrip struct {
	ID             int64
	CodAmount      *decimal.Decimal
	CodCollectedAt sql.NullTime
	CreatedAt      sql.NullTime
	DeliveredAt    sql.NullTime
	DistanceKm     *decimal.Decimal
	FailedAt       sql.NullTime
	FailureReason  *string
	Notes          *string
	OtpCode        *string
	OutAt          sql.NullTime
	PhotoProofURL  *string
	PickedUpAt     sql.NullTime
	Status         *string
	TipAmount      *decimal.Decimal
	UpdatedAt      sql.NullTime
	DriverUserID   *int
	OrderID        *int64
}

type DeliveryIssue struct {
	ID            int64
	Description   *string
	IssueType     *string
	ReportedAt    sql.NullTime
	ResolvedAt    sql.NullTime
	DriverUserID  *int
	TripID        *int64
}

type Task struct {
	ID                  int64
	AssignedDepartment  *string
	CompletedAt         sql.NullTime
	CreatedAt           sql.NullTime
	Description         *string
	DueDate             *time.Time
	Priority            *string
	RelatedOrderID      *int64
	ResolutionNotes     *string
	Status              *string
	Title               *string
	UpdatedAt           sql.NullTime
	AssignedUserID      *int
	CompletedByUserID   *int
	CreatedByUserID     *int
}

type DiscountCampaign struct {
	ID                int64
	CategoryFilter    *string
	CreatedAt         sql.NullTime
	DecidedAt         sql.NullTime
	DecisionNotes     *string
	DiscountPercent   *decimal.Decimal
	EndsOn            *time.Time
	Name              *string
	StartsOn          *time.Time
	Status            *string
	DecidedByUserID   *int
	ProposedByUserID  *int
}

type RefundRequest struct {
	ID              int64
	Amount          *decimal.Decimal
	CreatedAt       sql.NullTime
	DecidedAt       sql.NullTime
	DecisionNotes   *string
	Reason          *string
	RequestType     *string
	Status          *string
	DecidedByUserID *int
	OrderID         *int64
	RaisedByUserID  *int
}

type ProductImage struct {
	ID        int64
	ProductID *int64
	ImageURL  *string
}
