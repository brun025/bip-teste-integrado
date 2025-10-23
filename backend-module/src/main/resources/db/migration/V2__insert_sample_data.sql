-- Dados iniciais para testes
INSERT INTO BENEFICIO (NOME, DESCRICAO, VALOR, ATIVO, VERSION) VALUES
('Vale Alimentação', 'Benefício para compra de alimentos', 1000.00, TRUE, 0),
('Vale Refeição', 'Benefício para refeições em restaurantes', 500.00, TRUE, 0),
('Vale Transporte', 'Benefício para despesas com transporte', 300.00, TRUE, 0),
('Auxílio Saúde', 'Plano de saúde empresarial', 2000.00, TRUE, 0),
('Vale Cultura', 'Benefício para atividades culturais', 150.00, TRUE, 0),
('Auxílio Home Office', 'Ajuda de custo para trabalho remoto', 200.00, TRUE, 0),
('Benefício Inativo', 'Este benefício está desativado', 100.00, FALSE, 0);

-- Verificação
SELECT * FROM BENEFICIO ORDER BY ID;