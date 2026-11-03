-- Etapa 6: calculadora de fio (amostras de nó e dados do cordão)

-- Quantos fios (pontas) cabem lado a lado em 1 cm: define quantos cordões cortar pela largura
ALTER TABLE material ADD COLUMN fios_por_cm DECIMAL(6,2);
-- Peso de 1 metro, para converter metros em gramas
ALTER TABLE material ADD COLUMN gramas_por_metro DECIMAL(10,3);
-- Quando a unidade não é metro (rolo, novelo…): quantos metros tem 1 unidade
ALTER TABLE material ADD COLUMN metros_por_unidade DECIMAL(12,3);

-- Amostra: um fio de X cm, trabalhado num nó, rendeu Y cm. Fator = X ÷ Y (o jeito de cada artesã dar o nó)
CREATE TABLE material_amostra (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id  BIGINT NOT NULL,
    nome_no      VARCHAR(80) NOT NULL,
    fio_cm       DECIMAL(8,2) NOT NULL,
    rendeu_cm    DECIMAL(8,2) NOT NULL,
    observacao   VARCHAR(200),
    criado_em    DATETIME(6) NOT NULL,
    CONSTRAINT fk_amostra_material FOREIGN KEY (material_id) REFERENCES material (id) ON DELETE CASCADE
);
CREATE INDEX ix_amostra_material ON material_amostra (material_id);
