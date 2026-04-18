-- H2 Database: schema + seed data for E-Commerce grocery app
-- Activated by Spring profile "h2" (see application-h2.properties)

DROP TABLE IF EXISTS "userRole";
DROP TABLE IF EXISTS "User";
DROP TABLE IF EXISTS "Role";

CREATE TABLE "User" (
    userid    INTEGER AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(255),
    email     VARCHAR(255),
    password  VARCHAR(255),
    createdat TIMESTAMP
);

CREATE TABLE "Role" (
    id          INTEGER AUTO_INCREMENT PRIMARY KEY,
    "fullName"  VARCHAR(255),
    role        VARCHAR(255),
    department  VARCHAR(255)
);

INSERT INTO "User" (name, email, password, createdat) VALUES
    ('Kunal Sharma', 'kunal12@example.com', 'Love_To_Day', '2026-04-15 22:28:41.017'),
    ('Pinky Jain',   'pinky@example.com',   'Pinky_123',   '2026-04-15 22:30:22.300');

INSERT INTO "Role" ("fullName", role, department) VALUES
    ('Keerthi Dhat',  'Manager',         'Fresh Organic Products'),
    ('Joe Jonnas',    'Manager',         'Fresh Products'),
    ('Steve Wooten',  'sales Manager',   'Fresh Products'),
    ('Pinky joe',     'sales Manager',   'Fresh Organic Products'),
    ('Aishwarya R',   'Store Organizer', 'All Fresh Products');

CREATE TABLE "userRole" (
    userroleid SERIAL PRIMARY KEY,
    userid     INTEGER NOT NULL REFERENCES "User"(userid),
    roleid     INTEGER NOT NULL REFERENCES "Role"(id)
);

INSERT INTO "userRole" (userid, roleid) VALUES
    (1, 2),   -- Kunal Sharma → Manager, Fresh Products (Joe Jonnas)
    (1, 3),   -- Kunal Sharma → sales Manager, Fresh Products (Steve Wooten)
    (2, 1);   -- Pinky Jain   → Manager, Fresh Organic Products (Keerthi Dhat)
