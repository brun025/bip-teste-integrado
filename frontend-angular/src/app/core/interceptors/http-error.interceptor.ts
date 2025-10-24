import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { ErrorResponse } from '../models/error-response.model';

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastr = inject(ToastrService);

  return next(req).pipe(
    catchError((error) => {
      const errorResponse: ErrorResponse = error.error;

      switch (error.status) {
        case 400: 
          handleBadRequest(errorResponse, toastr);
          break;

        case 404: 
          toastr.error(errorResponse.message || 'Recurso não encontrado', 'Erro');
          break;

        case 409: 
          toastr.warning(
            errorResponse.message || 'Registro modificado por outro usuário. Recarregue a página.',
            'Conflito de Concorrência',
            { timeOut: 5000 }
          );
          break;

        case 422: 
          toastr.error(errorResponse.message || 'Operação não pode ser realizada', 'Erro de Validação');
          break;

        case 500: 
          toastr.error('Erro interno do servidor. Contate o suporte.', 'Erro');
          break;

        default:
          toastr.error('Erro inesperado. Tente novamente.', 'Erro');
      }

      return throwError(() => error);
    })
  );
};

function handleBadRequest(errorResponse: ErrorResponse, toastr: ToastrService): void {
  if (errorResponse.errors) {
    const errorMessages = Object.entries(errorResponse.errors)
      .map(([field, message]) => `<b>${field}:</b> ${message}`)
      .join('<br>');

    toastr.error(errorMessages, 'Erro de Validação', {
      enableHtml: true,
      timeOut: 5000
    });
  } else {
    toastr.error(errorResponse.message || 'Requisição inválida', 'Erro');
  }
}
