export interface TransferRequest {
  fromId: number;
  toId: number;
  amount: number;
  idempotencyKey: string;
}
