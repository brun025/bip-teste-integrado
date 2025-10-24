import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { BeneficioService } from '../../../../core/services/beneficio.service';
import { UuidService } from '../../../../core/services/uuid.service';
import { Beneficio } from '../../../../core/models/beneficio.model';
import { TransferRequest } from '../../../../core/models/transfer-request.model';

export interface TransferDialogData {
  beneficioOrigem: Beneficio;
  todosBeneficios: Beneficio[];
}

@Component({
  selector: 'app-transfer-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule],
  templateUrl: './transfer-dialog.component.html',
  styleUrl: './transfer-dialog.component.css'
})
export class TransferDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(BeneficioService);
  private readonly uuidService = inject(UuidService);
  private readonly toastr = inject(ToastrService);
  private readonly dialogRef = inject(MatDialogRef<TransferDialogComponent>);

  form!: FormGroup;
  isLoading = signal(false);
  beneficiosDestino: Beneficio[] = [];
  saldoOrigem: number = 0;

  constructor(@Inject(MAT_DIALOG_DATA) public data: TransferDialogData) {}

  ngOnInit(): void {
    this.saldoOrigem = parseFloat(this.data.beneficioOrigem.valor);
    this.filterBeneficiosDestino();
    this.initForm();
  }

  private filterBeneficiosDestino(): void {
    this.beneficiosDestino = this.data.todosBeneficios.filter(
      b => b.id !== this.data.beneficioOrigem.id
    );
  }

  private initForm(): void {
    this.form = this.fb.group({
      toId: [null, [Validators.required]],
      amount: [
        0,
        [
          Validators.required,
          Validators.min(0.01),
          Validators.max(this.saldoOrigem)
        ]
      ]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.markAllAsTouched();
      this.toastr.warning('Preencha todos os campos corretamente', 'Atenção');
      return;
    }

    this.isLoading.set(true);

    const request: TransferRequest = {
      fromId: this.data.beneficioOrigem.id,
      toId: this.form.value.toId,
      amount: this.form.value.amount,
      idempotencyKey: this.uuidService.generate()
    };

    this.service.transfer(request).subscribe({
      next: () => {
        this.toastr.success('Transferência realizada com sucesso!', 'Sucesso');
        this.dialogRef.close(true);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  private markAllAsTouched(): void {
    Object.keys(this.form.controls).forEach(key => {
      this.form.get(key)?.markAsTouched();
    });
  }

  hasError(field: string, error: string): boolean {
    const control = this.form.get(field);
    return !!(control?.hasError(error) && control?.touched);
  }

  getErrorMessage(field: string): string {
    const control = this.form.get(field);

    if (!control || !control.touched) return '';

    if (control.hasError('required')) return 'Campo obrigatório';
    if (control.hasError('min')) return 'Valor deve ser maior que zero';
    if (control.hasError('max')) {
      return `Saldo insuficiente. Máximo: R$ ${this.saldoOrigem.toFixed(2)}`;
    }

    return '';
  }

  formatCurrency(value: string): string {
    const numValue = parseFloat(value);
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(numValue);
  }
}
