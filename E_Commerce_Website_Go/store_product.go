package main

import (
	"database/sql"
	"errors"
)

func (db *DB) FindAllProducts() ([]Product, error) {
	rows, err := db.Query(`SELECT id, name, description, price, stock, category_id, created_by_userid,
		supported_flours, supported_sweeteners FROM "Products" ORDER BY id ASC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Product{}
	for rows.Next() {
		var p Product
		if err := rows.Scan(&p.ID, &p.Name, &p.Description, &p.Price, &p.Stock,
			&p.CategoryID, &p.CreatedByUserID, &p.SupportedFlours, &p.SupportedSweeteners); err != nil {
			return nil, err
		}
		out = append(out, p)
	}
	return out, rows.Err()
}

func (db *DB) FindProductByID(id int64) (*Product, error) {
	row := db.QueryRow(
		`SELECT id, name, description, price, stock, category_id, created_by_userid,
		        supported_flours, supported_sweeteners FROM "Products" WHERE id=$1`,
		id,
	)
	var p Product
	if err := row.Scan(&p.ID, &p.Name, &p.Description, &p.Price, &p.Stock,
		&p.CategoryID, &p.CreatedByUserID, &p.SupportedFlours, &p.SupportedSweeteners); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &p, nil
}

func (db *DB) SaveProduct(p *Product) (*Product, error) {
	if p.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO "Products"(name, description, price, stock, category_id, created_by_userid,
			                        supported_flours, supported_sweeteners)
			 VALUES($1,$2,$3,$4,$5,$6,$7,$8) RETURNING id`,
			p.Name, p.Description, p.Price, p.Stock, p.CategoryID, p.CreatedByUserID,
			p.SupportedFlours, p.SupportedSweeteners,
		)
		return p, row.Scan(&p.ID)
	}
	_, err := db.Exec(
		`UPDATE "Products" SET name=$1, description=$2, price=$3, stock=$4, category_id=$5,
		   created_by_userid=$6, supported_flours=$7, supported_sweeteners=$8 WHERE id=$9`,
		p.Name, p.Description, p.Price, p.Stock, p.CategoryID, p.CreatedByUserID,
		p.SupportedFlours, p.SupportedSweeteners, p.ID,
	)
	return p, err
}

func (db *DB) FindCategoryByID(id int64) (*Category, error) {
	row := db.QueryRow(`SELECT id, name, description FROM "Categories" WHERE id=$1`, id)
	var c Category
	if err := row.Scan(&c.ID, &c.Name, &c.Description); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &c, nil
}

func (db *DB) SaveCategory(c *Category) (*Category, error) {
	if c.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO "Categories"(name, description) VALUES($1, $2) RETURNING id`,
			c.Name, c.Description,
		)
		return c, row.Scan(&c.ID)
	}
	_, err := db.Exec(`UPDATE "Categories" SET name=$1, description=$2 WHERE id=$3`,
		c.Name, c.Description, c.ID)
	return c, err
}

func (db *DB) FindImagesByProductID(pid int64) ([]ProductImage, error) {
	rows, err := db.Query(`SELECT id, product_id, image_url FROM "Product_Images" WHERE product_id=$1 ORDER BY id ASC`, pid)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []ProductImage{}
	for rows.Next() {
		var im ProductImage
		if err := rows.Scan(&im.ID, &im.ProductID, &im.ImageURL); err != nil {
			return nil, err
		}
		out = append(out, im)
	}
	return out, rows.Err()
}

func (db *DB) SaveProductImage(im *ProductImage) (*ProductImage, error) {
	if im.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO "Product_Images"(product_id, image_url) VALUES($1,$2) RETURNING id`,
			im.ProductID, im.ImageURL,
		)
		return im, row.Scan(&im.ID)
	}
	_, err := db.Exec(`UPDATE "Product_Images" SET product_id=$1, image_url=$2 WHERE id=$3`,
		im.ProductID, im.ImageURL, im.ID)
	return im, err
}

// SaveProductAvailable mirrors the cleanup-archive flow.
func (db *DB) SaveProductAvailable(name, description *string, price *interface{},
	stock *int, categoryID *int64, createdByUserID *int, imageURL *string) error {
	_, err := db.Exec(
		`INSERT INTO "Product_Available"(name, description, price, stock, category_id, created_by_userid, image_url)
		 VALUES($1,$2,$3,$4,$5,$6,$7)`,
		name, description, price, stock, categoryID, createdByUserID, imageURL,
	)
	return err
}
