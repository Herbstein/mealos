CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  apple_subject text NOT NULL UNIQUE CHECK (length(apple_subject) BETWEEN 1 AND 255),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE auth_challenges (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  nonce_hash text NOT NULL,
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz
);
CREATE INDEX auth_challenges_expiry ON auth_challenges(expires_at);

CREATE TABLE sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  access_hash text NOT NULL UNIQUE,
  refresh_hash text NOT NULL UNIQUE,
  access_expires_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK (access_expires_at <= expires_at)
);
CREATE INDEX sessions_user ON sessions(user_id);
CREATE INDEX sessions_expiry ON sessions(expires_at);

CREATE TABLE recipe_sources (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid REFERENCES users(id),
  kind text NOT NULL CHECK (kind IN ('original', 'url', 'photo', 'text', 'file', 'assistant', 'book')),
  url text, title text, author text, attribution text,
  permission_notes text,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Text IDs preserve the existing bundled and imported recipe identifiers.
CREATE TABLE recipes (
  id text PRIMARY KEY DEFAULT gen_random_uuid()::text CHECK (length(id) BETWEEN 1 AND 200),
  owner_id uuid REFERENCES users(id),
  source_id uuid REFERENCES recipe_sources(id),
  parent_recipe_id text,
  parent_version_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CHECK ((parent_recipe_id IS NULL) = (parent_version_id IS NULL)),
  CHECK (parent_recipe_id IS DISTINCT FROM id)
);
CREATE INDEX recipes_owner ON recipes(owner_id);

CREATE TABLE recipe_versions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  recipe_id text NOT NULL REFERENCES recipes(id),
  version integer NOT NULL CHECK (version > 0),
  creator_id uuid REFERENCES users(id),
  title text NOT NULL CHECK (length(trim(title)) BETWEEN 1 AND 300),
  content jsonb NOT NULL CHECK (jsonb_typeof(content) = 'object'),
  schema_version integer NOT NULL DEFAULT 1 CHECK (schema_version > 0),
  change_summary text,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(recipe_id, version), UNIQUE(recipe_id, id)
);
ALTER TABLE recipes ADD FOREIGN KEY(parent_recipe_id, parent_version_id)
  REFERENCES recipe_versions(recipe_id, id);

CREATE FUNCTION reject_version_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Recipe versions are immutable; create a new version'; END;
$$;
CREATE TRIGGER immutable_recipe_versions BEFORE UPDATE ON recipe_versions
  FOR EACH ROW EXECUTE FUNCTION reject_version_update();

CREATE TABLE recipe_drafts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES users(id),
  recipe_id text NOT NULL REFERENCES recipes(id),
  base_version_id uuid,
  revision integer NOT NULL DEFAULT 1 CHECK (revision > 0),
  content jsonb NOT NULL CHECK (jsonb_typeof(content) = 'object'),
  updated_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY(recipe_id, base_version_id) REFERENCES recipe_versions(recipe_id, id)
);
CREATE INDEX recipe_drafts_owner ON recipe_drafts(owner_id);

CREATE TABLE publications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  recipe_id text NOT NULL REFERENCES recipes(id),
  version_id uuid NOT NULL,
  visibility text NOT NULL CHECK (visibility IN ('public', 'unlisted')),
  review_state text NOT NULL DEFAULT 'pending' CHECK (review_state IN ('pending', 'approved', 'rejected')),
  share_token_hash text UNIQUE,
  published_at timestamptz NOT NULL DEFAULT now(),
  withdrawn_at timestamptz,
  FOREIGN KEY(recipe_id, version_id) REFERENCES recipe_versions(recipe_id, id),
  CHECK (visibility <> 'unlisted' OR share_token_hash IS NOT NULL)
);
CREATE UNIQUE INDEX one_active_publication ON publications(recipe_id)
  WHERE withdrawn_at IS NULL;

CREATE TABLE library_entries (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  recipe_id text NOT NULL REFERENCES recipes(id),
  version_id uuid NOT NULL,
  favorite boolean NOT NULL DEFAULT false,
  rating integer CHECK (rating BETWEEN 1 AND 5),
  notes text NOT NULL DEFAULT '',
  revision integer NOT NULL DEFAULT 1 CHECK (revision > 0),
  deleted_at timestamptz,
  PRIMARY KEY(user_id, recipe_id),
  FOREIGN KEY(recipe_id, version_id) REFERENCES recipe_versions(recipe_id, id)
);

CREATE TABLE collections (
  id text PRIMARY KEY, title text NOT NULL
);
CREATE TABLE collection_entries (
  collection_id text NOT NULL REFERENCES collections(id),
  publication_id uuid NOT NULL REFERENCES publications(id),
  position integer NOT NULL DEFAULT 0,
  PRIMARY KEY(collection_id, publication_id)
);

CREATE TABLE meal_plan_entries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  day date NOT NULL,
  recipe_id text NOT NULL REFERENCES recipes(id),
  version_id uuid NOT NULL,
  portion text NOT NULL CHECK (portion IN ('ONE', 'TWO', 'DINNER_PLUS_LUNCH')),
  revision integer NOT NULL DEFAULT 1 CHECK (revision > 0),
  deleted_at timestamptz,
  FOREIGN KEY(recipe_id, version_id) REFERENCES recipe_versions(recipe_id, id)
);
CREATE UNIQUE INDEX one_meal_per_day ON meal_plan_entries(user_id, day) WHERE deleted_at IS NULL;

CREATE TABLE personal_selections (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  kind text NOT NULL CHECK (kind IN ('pantry', 'grocery')),
  item_key text NOT NULL,
  selected boolean NOT NULL,
  revision integer NOT NULL DEFAULT 1 CHECK (revision > 0),
  PRIMARY KEY(user_id, kind, item_key)
);

CREATE TABLE import_jobs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  operation_id uuid NOT NULL,
  state text NOT NULL DEFAULT 'queued' CHECK (state IN ('queued', 'extracting', 'needs_review', 'saved', 'failed')),
  input jsonb NOT NULL CHECK (jsonb_typeof(input) = 'object'),
  result jsonb,
  warnings jsonb NOT NULL DEFAULT '[]' CHECK (jsonb_typeof(warnings) = 'array'),
  attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  lease_until timestamptz,
  cost_usd numeric(12,6) NOT NULL DEFAULT 0 CHECK (cost_usd >= 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(user_id, operation_id)
);
CREATE INDEX import_jobs_recovery ON import_jobs(state, lease_until);

CREATE TABLE media_assets (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES users(id),
  version_id uuid REFERENCES recipe_versions(id),
  storage_key text NOT NULL UNIQUE,
  source_id uuid REFERENCES recipe_sources(id),
  permission_notes text,
  expires_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- No client database grants: all access is mediated by the backend. Recipe
-- editing, publication and sync endpoints will enforce their own transactions.
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;
