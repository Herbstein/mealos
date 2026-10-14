-- Private sync snapshots are independent of the publication/catalog schema.
CREATE TABLE library_sync (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL DEFAULT 0 CHECK (revision >= 0),
  library jsonb,
  CHECK (library IS NULL OR jsonb_typeof(library) = 'object')
);
-- Receipts make a lost response safe to retry even after another device writes.
CREATE TABLE library_sync_operations (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  operation_id uuid NOT NULL,
  request_hash text NOT NULL,
  revision bigint NOT NULL,
  PRIMARY KEY (user_id, operation_id)
);
