export interface Beneficio {
  id: number;
  nome: string;
  descricao: string;
  valor: string;
  ativo: boolean;
  version: number;
}

export interface BeneficioCreateDto {
  nome: string;
  descricao: string;
  valor: number;
  ativo: boolean;
}

export interface BeneficioUpdateDto extends BeneficioCreateDto {
  version: number;
}
