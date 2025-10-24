import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'beneficios',
    pathMatch: 'full'
  },
  {
    path: 'beneficios',
    loadChildren: () => import('./features/beneficios/beneficios.routes').then(m => m.BENEFICIOS_ROUTES)
  },
  {
    path: '**',
    redirectTo: 'beneficios'
  }
];
