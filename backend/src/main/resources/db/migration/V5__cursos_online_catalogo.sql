-- Etapa 5: cursos online e catálogo de peças no site

CREATE TABLE curso (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo      VARCHAR(140) NOT NULL,
    resumo      VARCHAR(220),
    descricao   TEXT,
    capa        VARCHAR(200),
    valor       DECIMAL(12,2) NOT NULL,
    nivel       VARCHAR(40),
    -- O que a aluna precisa ter em casa
    materiais   TEXT,
    publicado   BOOLEAN NOT NULL DEFAULT FALSE,
    ordem       INT NOT NULL DEFAULT 0,
    criado_em   DATETIME(6) NOT NULL
);

CREATE TABLE curso_aula (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    curso_id         BIGINT NOT NULL,
    titulo           VARCHAR(160) NOT NULL,
    descricao        TEXT,
    -- Link do YouTube (não listado), Vimeo ou Google Drive
    video_url        VARCHAR(500),
    duracao_minutos  INT,
    -- Aula aberta para quem ainda não comprou (amostra)
    gratuita         BOOLEAN NOT NULL DEFAULT FALSE,
    ordem            INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aula_curso FOREIGN KEY (curso_id) REFERENCES curso (id)
);
CREATE INDEX idx_aula_curso ON curso_aula (curso_id, ordem);

CREATE TABLE matricula (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    curso_id     BIGINT NOT NULL,
    cliente_id   BIGINT,
    -- Código do link de acesso da aluna
    codigo       VARCHAR(36) NOT NULL,
    nome         VARCHAR(140) NOT NULL,
    whatsapp     VARCHAR(20) NOT NULL,
    email        VARCHAR(160),
    status       VARCHAR(20) NOT NULL,
    origem       VARCHAR(10) NOT NULL,
    liberada_em  DATETIME(6),
    criado_em    DATETIME(6) NOT NULL,
    CONSTRAINT uk_matricula_codigo UNIQUE (codigo),
    CONSTRAINT fk_mat_curso FOREIGN KEY (curso_id) REFERENCES curso (id),
    CONSTRAINT fk_mat_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);
CREATE INDEX idx_mat_curso ON matricula (curso_id);

CREATE TABLE matricula_aula (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    matricula_id  BIGINT NOT NULL,
    aula_id       BIGINT NOT NULL,
    concluida_em  DATETIME(6) NOT NULL,
    CONSTRAINT uk_mat_aula UNIQUE (matricula_id, aula_id),
    CONSTRAINT fk_ma_matricula FOREIGN KEY (matricula_id) REFERENCES matricula (id),
    CONSTRAINT fk_ma_aula FOREIGN KEY (aula_id) REFERENCES curso_aula (id) ON DELETE CASCADE
);

ALTER TABLE lancamento ADD COLUMN matricula_id BIGINT;
ALTER TABLE lancamento ADD CONSTRAINT fk_lanc_matricula FOREIGN KEY (matricula_id) REFERENCES matricula (id);

-- Catálogo: a peça da ficha técnica aparece no site com texto e fotos próprios
ALTER TABLE produto ADD COLUMN no_catalogo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE produto ADD COLUMN mostrar_preco BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE produto ADD COLUMN texto_catalogo TEXT;
ALTER TABLE produto ADD COLUMN prazo_producao VARCHAR(80);
ALTER TABLE produto ADD COLUMN ordem_catalogo INT NOT NULL DEFAULT 0;

CREATE TABLE produto_foto (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id  BIGINT NOT NULL,
    arquivo     VARCHAR(200) NOT NULL,
    ordem       INT NOT NULL DEFAULT 0,
    criado_em   DATETIME(6) NOT NULL,
    CONSTRAINT fk_pfoto_produto FOREIGN KEY (produto_id) REFERENCES produto (id)
);
CREATE INDEX idx_pfoto_produto ON produto_foto (produto_id, ordem);
