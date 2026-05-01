DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_member_nickname'
    ) THEN
        ALTER TABLE member ADD CONSTRAINT uk_member_nickname UNIQUE (nickname);
    END IF;
END $$;