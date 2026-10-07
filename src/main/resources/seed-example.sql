START TRANSACTION;
INSERT INTO users (name, created_at) VALUES ('테스트 사용자', CURRENT_TIMESTAMP);
SET @seed_user_id = LAST_INSERT_ID();
INSERT INTO user_points (user_id, balance, updated_at)
VALUES (@seed_user_id, 0, CURRENT_TIMESTAMP);
INSERT INTO menus (name, price, created_at) VALUES
('아메리카노', 4500, CURRENT_TIMESTAMP),
('카페라떼', 5000, CURRENT_TIMESTAMP),
('바닐라라떼', 5500, CURRENT_TIMESTAMP);
COMMIT;
SELECT @seed_user_id AS user_id;
SELECT id AS menu_id, name, price FROM menus ORDER BY id;
