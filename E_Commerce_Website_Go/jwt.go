package main

import (
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
)

type JwtUtil struct {
	secret       []byte
	expirationMs int64
}

func NewJwtUtil(secret string, expirationMs int64) *JwtUtil {
	return &JwtUtil{secret: []byte(secret), expirationMs: expirationMs}
}

func (j *JwtUtil) GenerateToken(userid int, email string, roles, departments []string) (string, error) {
	now := time.Now()
	claims := jwt.MapClaims{
		"sub":         email,
		"userid":      userid,
		"roles":       roles,
		"departments": departments,
		"iat":         now.Unix(),
		"exp":         now.Add(time.Duration(j.expirationMs) * time.Millisecond).Unix(),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(j.secret)
}

func (j *JwtUtil) Parse(token string) (jwt.MapClaims, error) {
	parsed, err := jwt.Parse(token, func(t *jwt.Token) (interface{}, error) {
		return j.secret, nil
	})
	if err != nil {
		return nil, err
	}
	if claims, ok := parsed.Claims.(jwt.MapClaims); ok && parsed.Valid {
		return claims, nil
	}
	return nil, jwt.ErrTokenInvalidClaims
}

const ctxClaimsKey = "jwtClaims"

// JwtMiddleware parses the bearer token (if any) and stores claims in context.
// Mirrors JwtFilter.java — tolerates absence/badness; does not 401 itself.
func JwtMiddleware(j *JwtUtil) gin.HandlerFunc {
	return func(c *gin.Context) {
		auth := c.GetHeader("Authorization")
		if strings.HasPrefix(auth, "Bearer ") {
			token := strings.TrimPrefix(auth, "Bearer ")
			if claims, err := j.Parse(token); err == nil {
				c.Set(ctxClaimsKey, claims)
			}
		}
		c.Next()
	}
}

// AuthGate mirrors SecurityConfig.java — public endpoints, everything else needs auth.
func AuthGate() gin.HandlerFunc {
	publicExact := map[string]struct{}{
		"/login":    {},
		"/register": {},
	}
	return func(c *gin.Context) {
		path := c.Request.URL.Path
		method := c.Request.Method
		if method == "OPTIONS" {
			c.Next()
			return
		}
		if _, ok := publicExact[path]; ok {
			c.Next()
			return
		}
		if method == "GET" && path == "/products" {
			c.Next()
			return
		}
		if _, ok := c.Get(ctxClaimsKey); !ok {
			c.AbortWithStatusJSON(401, gin.H{"error": "unauthorized"})
			return
		}
		c.Next()
	}
}
