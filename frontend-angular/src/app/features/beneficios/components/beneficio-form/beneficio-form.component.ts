import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { BeneficioService } from '../../../../core/services/beneficio.service';
import { Beneficio, BeneficioCreateDto, BeneficioUpdateDto } from '../../../../core/models/beneficio.model';

@Component({
  selector: 'app-beneficio-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './beneficio-form.component.html',
  styleUrl: './beneficio-form.component.css'
})
export class BeneficioFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(BeneficioService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toastr = inject(ToastrService);

  form!: FormGroup;
  isEditMode = signal(false);
  isLoading = signal(false);
  beneficioId: number | null = null;
  currentVersion: number = 0;

  ngOnInit(): void {
    this.initForm();
    this.checkEditMode();
  }

  private initForm(): void {
    this.form = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      descricao: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]],
      valor: [0, [Validators.required, Validators.min(0.01)]],
      ativo: [true]
    });
  }

  private checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.beneficioId = +id;
      this.isEditMode.set(true);
      this.loadBeneficio(this.beneficioId);
    }
  }

  private loadBeneficio(id: number): void {
    this.isLoading.set(true);

    this.service.findById(id).subscribe({
      next: (beneficio: Beneficio) => {
        this.currentVersion = beneficio.version;
        this.form.patchValue({
          nome: beneficio.nome,
          descricao: beneficio.descricao,
          valor: parseFloat(beneficio.valor),
          ativo: beneficio.ativo
        });
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.router.navigate(['/beneficios']);
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.markAllAsTouched();
      this.toastr.warning('Por favor, preencha todos os campos corretamente', 'Atenção');
      return;
    }

    this.isLoading.set(true);

    if (this.isEditMode()) {
      this.updateBeneficio();
    } else {
      this.createBeneficio();
    }
  }

  private createBeneficio(): void {
    const dto: BeneficioCreateDto = this.form.value;

    this.service.create(dto).subscribe({
      next: () => {
        this.toastr.success('Benefício criado com sucesso!', 'Sucesso');
        this.router.navigate(['/beneficios']);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  private updateBeneficio(): void {
    const dto: BeneficioUpdateDto = {
      ...this.form.value,
      version: this.currentVersion
    };

    this.service.update(this.beneficioId!, dto).subscribe({
      next: () => {
        this.toastr.success('Benefício atualizado com sucesso!', 'Sucesso');
        this.router.navigate(['/beneficios']);
      },
      error: (error) => {
        this.isLoading.set(false);

        if (error.status === 409) {
          this.toastr.info('Recarregando dados atualizados...', 'Informação');
          this.loadBeneficio(this.beneficioId!);
        }
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/beneficios']);
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
    if (control.hasError('minlength')) {
      const minLength = control.errors?.['minlength']?.requiredLength;
      return `Mínimo de ${minLength} caracteres`;
    }
    if (control.hasError('maxlength')) {
      const maxLength = control.errors?.['maxlength']?.requiredLength;
      return `Máximo de ${maxLength} caracteres`;
    }
    if (control.hasError('min')) {
      return 'Valor deve ser maior que zero';
    }

    return '';
  }
}
