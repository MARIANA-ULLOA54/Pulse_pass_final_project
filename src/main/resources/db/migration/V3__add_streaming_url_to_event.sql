-- V3: evento hibrido (URL de streaming opcional)
ALTER TABLE events ADD COLUMN streaming_url VARCHAR(500);
