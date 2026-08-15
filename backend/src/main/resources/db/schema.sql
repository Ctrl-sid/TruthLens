-- TruthLens PostgreSQL / SQL Database Schema

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) DEFAULT 'ROLE_USER',
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, WARNED, BANNED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Verified Reliable Sources Whitelist Table
CREATE TABLE IF NOT EXISTS verified_sources (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(150) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    credibility_score INT NOT NULL DEFAULT 90, -- 0 to 100
    category VARCHAR(50) NOT NULL, -- News, FactChecker, Government, Scientific
    bias_rating VARCHAR(50), -- Center, Slight Left, Slight Right
    verified_url VARCHAR(255)
);

-- Fact Check Verification History Table
CREATE TABLE IF NOT EXISTS fact_check_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    input_type VARCHAR(20) NOT NULL, -- TEXT, URL, IMAGE
    input_content TEXT NOT NULL,
    claim_summary VARCHAR(500),
    genuineness_score INT NOT NULL, -- 0 to 100
    verdict VARCHAR(50) NOT NULL, -- GENUINE, MOSTLY_GENUINE, MIXED_MISLEADING, LIKELY_FAKE, FABRICATED
    rationale TEXT NOT NULL,
    nlp_metrics_json TEXT, -- NLP diagnostics: sentiment, entities, clickbait score
    source_evidence_json TEXT, -- Cross-referenced sources & links
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User-Admin Messaging Gateway Table
CREATE TABLE IF NOT EXISTS admin_messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    receiver_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    claim_id BIGINT REFERENCES fact_check_history(id) ON DELETE SET NULL,
    subject VARCHAR(200) NOT NULL,
    message_text TEXT NOT NULL,
    claim_context_summary VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Claim Feedback & Rating Table
CREATE TABLE IF NOT EXISTS claim_feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    claim_id BIGINT REFERENCES fact_check_history(id) ON DELETE CASCADE,
    rating INT, -- 1 to 5 stars
    flag_reason VARCHAR(100), -- INACCURATE_FACT, CULTURALLY_INAPPROPRIATE, PERSONAL_BIAS, OTHER
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Initial Reliable Sources Seed Data
INSERT INTO verified_sources (domain, name, credibility_score, category, bias_rating, verified_url) 
VALUES 
('reuters.com', 'Reuters News', 98, 'News Agency', 'Center', 'https://www.reuters.com/fact-check'),
('apnews.com', 'Associated Press', 97, 'News Agency', 'Center', 'https://apnews.com/ap-fact-check'),
('snopes.com', 'Snopes Fact Check', 95, 'FactChecker', 'Center', 'https://www.snopes.com'),
('politifact.com', 'PolitiFact', 94, 'FactChecker', 'Center', 'https://www.politifact.com'),
('factcheck.org', 'FactCheck.org', 96, 'FactChecker', 'Center', 'https://www.factcheck.org'),
('bbc.com', 'BBC Reality Check', 94, 'News Agency', 'Center', 'https://www.bbc.com/news/reality_check'),
('nature.com', 'Nature Journal', 99, 'Scientific', 'Center', 'https://www.nature.com'),
('who.int', 'World Health Organization', 96, 'Official Organization', 'Center', 'https://www.who.int')
ON CONFLICT (domain) DO NOTHING;
