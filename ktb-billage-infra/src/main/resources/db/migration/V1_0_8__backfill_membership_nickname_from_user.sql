UPDATE membership m
JOIN users u ON u.id = m.user_id
SET m.nickname = LEFT(u.nickname, 20)
WHERE m.nickname IS NULL
  AND u.nickname IS NOT NULL;
