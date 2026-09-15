CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    repository_url TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    analyzed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS source_classes (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    package_name VARCHAR(500),
    class_name VARCHAR(255) NOT NULL,
    qualified_name VARCHAR(1000) NOT NULL,
    component_type VARCHAR(30) NOT NULL,
    file_path TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS source_methods (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES source_classes(id) ON DELETE CASCADE,
    method_name VARCHAR(255) NOT NULL,
    signature VARCHAR(2000) NOT NULL,
    return_type VARCHAR(1000) NOT NULL,
    start_line INTEGER NOT NULL,
    end_line INTEGER NOT NULL,
    source_code TEXT NOT NULL,
    http_method VARCHAR(20),
    request_path VARCHAR(1000)
);

ALTER TABLE source_methods ADD COLUMN IF NOT EXISTS signature VARCHAR(2000);
ALTER TABLE source_methods ADD COLUMN IF NOT EXISTS return_type VARCHAR(1000);
ALTER TABLE source_methods ADD COLUMN IF NOT EXISTS start_line INTEGER;
ALTER TABLE source_methods ADD COLUMN IF NOT EXISTS end_line INTEGER;
ALTER TABLE source_methods ADD COLUMN IF NOT EXISTS source_code TEXT;

CREATE INDEX IF NOT EXISTS idx_source_classes_project_id ON source_classes(project_id);
CREATE INDEX IF NOT EXISTS idx_source_methods_class_id ON source_methods(class_id);
