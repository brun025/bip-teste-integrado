import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { BeneficioService } from '../../../../core/services/beneficio.service';
import { Beneficio } from '../../../../core/models/beneficio.model';
import { TransferDialogComponent } from '../transfer-dialog/transfer-dialog.component';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-beneficio-list',
  standalone: true,
  imports: [CommonModule, MatDialogModule],
  templateUrl: './beneficio-list.component.html',
  styleUrl: './beneficio-list.component.css'
})
export class BeneficioListComponent implements OnInit {
  private readonly service = inject(BeneficioService);
  private readonly router = inject(Router);
  private readonly toastr = inject(ToastrService);
  private readonly dialog = inject(MatDialog);

  beneficios = signal<Beneficio[]>([]);
  isLoading = signal(false);

  ngOnInit(): void {
    this.loadBeneficios();
  }

  loadBeneficios(): void {
    this.isLoading.set(true);

    this.service.findAll().subscribe({
      next: (data) => {
        this.beneficios.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  onCreate(): void {
    this.router.navigate(['/beneficios/novo']);
  }

  onEdit(id: number): void {
    this.router.navigate(['/beneficios/editar', id]);
  }

  onTransfer(beneficio: Beneficio): void {
    const dialogRef = this.dialog.open(TransferDialogComponent, {
      width: '500px',
      data: {
        beneficioOrigem: beneficio,
        todosBeneficios: this.beneficios()
      }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        this.loadBeneficios();
      }
    });
  }

  onDelete(beneficio: Beneficio): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirmar Exclusão',
        message: `Tem certeza que deseja excluir o benefício "${beneficio.nome}"?`,
        confirmText: 'Excluir',
        cancelText: 'Cancelar'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.deleteBeneficio(beneficio.id);
      }
    });
  }

  private deleteBeneficio(id: number): void {
    this.service.delete(id).subscribe({
      next: () => {
        this.toastr.success('Benefício excluído com sucesso!', 'Sucesso');
        this.loadBeneficios();
      }
    });
  }

  formatCurrency(value: string): string {
    const numValue = parseFloat(value);
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(numValue);
  }
}
