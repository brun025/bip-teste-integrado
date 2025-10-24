import { Routes } from '@angular/router';
import { BeneficioListComponent } from './components/beneficio-list/beneficio-list.component';
import { BeneficioFormComponent } from './components/beneficio-form/beneficio-form.component';

export const BENEFICIOS_ROUTES: Routes = [
  {
    path: '',
    component: BeneficioListComponent
  },
  {
    path: 'novo',
    component: BeneficioFormComponent
  },
  {
    path: 'editar/:id',
    component: BeneficioFormComponent
  }
];
