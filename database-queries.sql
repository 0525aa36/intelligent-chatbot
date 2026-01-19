-- PostgreSQL 데이터베이스 쿼리 모음
-- IntelliJ Database Console에서 사용

-- 연결 정보:
-- Host: localhost
-- Port: 53413
-- Database: mydatabase
-- User: myuser
-- Password: secret

-------------------------------------------------
-- 테이블 구조 확인
-------------------------------------------------

-- 모든 테이블 목록
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public';

-- users 테이블 구조
\d users;

-- chats 테이블 구조
\d chats;

-- threads 테이블 구조
\d threads;

-------------------------------------------------
-- 데이터 조회
-------------------------------------------------

-- 1. 모든 사용자 조회
SELECT id, email, name, role, created_at
FROM users
ORDER BY created_at DESC;

-- 2. 최근 채팅 10개 조회
SELECT
    c.id,
    c.thread_id,
    u.name as user_name,
    c.question,
    LEFT(c.answer, 100) as answer_preview,
    c.created_at
FROM chats c
JOIN threads t ON c.thread_id = t.id
JOIN users u ON t.user_id = u.id
ORDER BY c.created_at DESC
LIMIT 10;

-- 3. 스레드별 채팅 개수
SELECT
    t.id as thread_id,
    u.name as user_name,
    COUNT(c.id) as chat_count,
    t.created_at,
    t.last_activity_at
FROM threads t
JOIN users u ON t.user_id = u.id
LEFT JOIN chats c ON c.thread_id = t.id
GROUP BY t.id, u.name, t.created_at, t.last_activity_at
ORDER BY t.last_activity_at DESC;

-- 4. 사용자별 채팅 통계
SELECT
    u.id,
    u.name,
    u.email,
    COUNT(DISTINCT t.id) as thread_count,
    COUNT(c.id) as total_chats,
    u.created_at as joined_at
FROM users u
LEFT JOIN threads t ON t.user_id = u.id
LEFT JOIN chats c ON c.thread_id = t.id
GROUP BY u.id, u.name, u.email, u.created_at
ORDER BY total_chats DESC;

-- 5. 특정 스레드의 전체 대화 내역 (스레드 ID = 3)
SELECT
    c.id,
    c.question,
    c.answer,
    c.created_at
FROM chats c
WHERE c.thread_id = 3
ORDER BY c.created_at ASC;

-- 6. Gemini API 응답이 긴 채팅 찾기 (500자 이상)
SELECT
    id,
    thread_id,
    question,
    LENGTH(answer) as answer_length,
    LEFT(answer, 100) as answer_preview,
    created_at
FROM chats
WHERE LENGTH(answer) > 500
ORDER BY answer_length DESC;

-- 7. 오늘 생성된 채팅
SELECT
    c.id,
    u.name as user_name,
    c.question,
    LEFT(c.answer, 50) as answer_preview,
    c.created_at
FROM chats c
JOIN threads t ON c.thread_id = t.id
JOIN users u ON t.user_id = u.id
WHERE DATE(c.created_at) = CURRENT_DATE
ORDER BY c.created_at DESC;

-------------------------------------------------
-- 데이터 수정 (주의해서 사용)
-------------------------------------------------

-- 특정 채팅 삭제
-- DELETE FROM chats WHERE id = 1;

-- 특정 스레드와 관련 채팅 모두 삭제
-- DELETE FROM chats WHERE thread_id = 1;
-- DELETE FROM threads WHERE id = 1;

-- 모든 데이터 초기화 (주의!)
-- TRUNCATE TABLE chats CASCADE;
-- TRUNCATE TABLE threads CASCADE;
-- TRUNCATE TABLE feedbacks CASCADE;

-------------------------------------------------
-- 통계 쿼리
-------------------------------------------------

-- 전체 통계
SELECT
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM threads) as total_threads,
    (SELECT COUNT(*) FROM chats) as total_chats,
    (SELECT COUNT(*) FROM feedbacks) as total_feedbacks;

-- 최근 24시간 활동
SELECT
    COUNT(DISTINCT u.id) as active_users,
    COUNT(DISTINCT t.id) as active_threads,
    COUNT(c.id) as new_chats
FROM chats c
JOIN threads t ON c.thread_id = t.id
JOIN users u ON t.user_id = u.id
WHERE c.created_at >= NOW() - INTERVAL '24 hours';

-- 평균 응답 길이
SELECT
    ROUND(AVG(LENGTH(answer))) as avg_answer_length,
    MIN(LENGTH(answer)) as min_answer_length,
    MAX(LENGTH(answer)) as max_answer_length
FROM chats;
